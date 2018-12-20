import java.io.IOException
import java.nio.charset.Charset
import java.nio.file.{Files, Path, Paths}
import java.util.Collections

import scala.collection.JavaConverters._

import ru.ifmo.ds.Database
import ru.ifmo.ds.io.Json
import ru.ifmo.ds.ops.FindDifferences
import ru.ifmo.ds.ops.FindDifferences.DifferenceListener
import ru.ifmo.ds.stat.KolmogorovSmirnov

vars.stateFileName = "state.properties"
vars.singleStep = false
vars.dataRoot = "performance-data"

val DataSubdirectoryRaw = "raw"
val DataSubdirectoryConsolidated = "merged"
val ListOfAlgorithms = "changed.lst"
val BasicPValue = 1e-15
val KeyValue = "primaryMetric.rawData"
val KeyCats = Seq("benchmark", "params.d", "params.f", "params.n")
val KeyAlgorithm = "params.algorithmId"

class CompareListener(p: Double) extends DifferenceListener {
  private val differingAlgorithms, nonDifferingAlgorithms = IndexedSeq.newBuilder[String]
  def result(): IndexedSeq[String] = differingAlgorithms.result() ++ nonDifferingAlgorithms.result()
  override def keyValuesDoNotMatch(slice: Map[String, Option[String]], key: String,
                                   onlyLeft: Set[Option[String]],
                                   onlyRight: Set[Option[String]]): Unit = {
    // It can be that the new commit features new algorithms which did not exist yet. These need to be added.
    // It can be that the new commit deletes some of the old algorithms. These need not to be added.
    if (key == KeyAlgorithm) {
      onlyRight.foreach(differingAlgorithms ++= _)
    }
  }
  override def kolmogorovSmirnovFailure(slice: Map[String, Option[String]],
                                        key: String, leftValues: Seq[String], rightValues: Seq[String],
                                        exception: Throwable): Unit = throw exception
  override def kolmogorovSmirnovResult(slice: Map[String, Option[String]],
                                       key: String, leftValues: Seq[Double], rightValues: Seq[Double],
                                       result: KolmogorovSmirnov.Result): Unit = {}
  override def sliceStatistics(slice: Map[String, Option[String]],
                               key: String, statistics: Seq[KolmogorovSmirnov.Result]): Unit = {
    if (slice.keySet == Set(KeyAlgorithm)) {
      slice(KeyAlgorithm) match {
        case None =>
        case Some(algorithm) =>
          val stat = KolmogorovSmirnov.rankSumOnMultipleOutcomes(statistics)
          if (stat < p) {
            differingAlgorithms += s"$algorithm $stat"
          } else {
            nonDifferingAlgorithms += s"#$algorithm $stat"
          }
      }
    }
  }
}

def readListOfAlgorithms(file: Path): Set[String] = {
  def indexOrEnd(i: Int, s: String): Int = if (i < 0) s.length else i
  def firstToken(s: String): String = s.substring(0, indexOrEnd(s.indexOf(' '), s))
  Files.readAllLines(file).asScala.filterNot(_.startsWith("#")).map(firstToken).toSet
}

class ComputeMinimal(useKey: String) extends Phase(s"phase.minimal-$useKey.compute") {
  override def execute(curr: Path, prev: Option[Path]): Unit = {
    val currentPhaseOut = s"minimal-$useKey.json"
    val rawDir = curr.resolve(DataSubdirectoryRaw)
    Files.createDirectories(rawDir)
    val outputFile = rawDir.resolve(currentPhaseOut)
    val listOfAlgorithms = curr.resolve(ListOfAlgorithms)
    val algorithms = if (Files.exists(listOfAlgorithms)) {
      readListOfAlgorithms(listOfAlgorithms).mkString("--algo=", ",", "")
    } else ""
    if (algorithms == "--algo=") {
      // When an empty parameter list is given, JMH thinks one shall use the compiled-in parameters, which fails.
      // Write an empty JSON file instead.
      Files.write(outputFile, Collections.singletonList("[]"))
    } else {
      val pb = new ProcessBuilder()
      pb.command("sbt",
        "project benchmarking",
        s"jmh:runMain ru.ifmo.nds.jmh.main.Minimal $algorithms --use=$useKey --out=${outputFile.toAbsolutePath}")
      pb.inheritIO().directory(curr.toFile)
      val exitCode = pb.start().waitFor()
      if (exitCode != 0) {
        throw new IOException("Exit code " + exitCode)
      }
    }
    gzipJson(outputFile)
  }
}

object CompareMinimal extends Phase("phase.minimal-min.compare") {
  override def execute(curr: Path, prevOption: Option[Path]): Unit = {
    val currentPhaseIn = "minimal-min.json.gz"
    val listOfAlgorithms = curr.resolve(ListOfAlgorithms)
    prevOption match {
      case Some(prev) =>
        val oldDB = Json.fromFile(prev.resolve(DataSubdirectoryConsolidated).resolve(currentPhaseIn).toFile)
        val newDB = Json.fromFile(curr.resolve(DataSubdirectoryRaw).resolve(currentPhaseIn).toFile)
        val commonAlgorithms = oldDB.valuesUnderKey(KeyAlgorithm).intersect(newDB.valuesUnderKey(KeyAlgorithm))
        val listener = new CompareListener(BasicPValue / commonAlgorithms.size)
        FindDifferences.traverse(oldDB, newDB, KeyAlgorithm +: KeyCats, KeyValue, listener)
        Files.write(listOfAlgorithms, listener.result().asJava, Charset.defaultCharset())
      case None =>
        // No previous runs detected. Need to write all algorithms to the file
        val file = curr.resolve(DataSubdirectoryRaw).resolve(currentPhaseIn)
        val allAlgorithms = Json.fromFile(file.toFile).valuesUnderKey(KeyAlgorithm).flatMap(_.iterator).toIndexedSeq.sorted
        Files.write(listOfAlgorithms, allAlgorithms.asJava, Charset.defaultCharset())
    }
  }
}

class Consolidate(key: String) extends Phase(s"phase.$key.consolidate") {
  override def execute(curr: Path, prevOption: Option[Path]): Unit = {
    val target = curr.resolve(DataSubdirectoryConsolidated)
    val currentPhaseOut = key + ".json.gz"
    Files.createDirectories(target)
    val trg = target.resolve(currentPhaseOut)
    prevOption match {
      case None =>
        // no previous runs - just copy a file over
        val src = curr.resolve(DataSubdirectoryRaw).resolve(currentPhaseOut)
        if (!Files.isSameFile(trg, src)) {
          Files.deleteIfExists(trg)
          Files.createLink(trg, src)
        }
      case Some(prev) =>
        val oldDB = Json.fromFile(prev.resolve(DataSubdirectoryConsolidated).resolve(currentPhaseOut).toFile)
        val newDB = Json.fromFile(curr.resolve(DataSubdirectoryRaw).resolve(currentPhaseOut).toFile)
        val differingAlgorithms = readListOfAlgorithms(curr.resolve(ListOfAlgorithms))
        val oldDBFiltered = oldDB.filter(e => e.contains(KeyAlgorithm) && !differingAlgorithms.contains(e(KeyAlgorithm)))
        val merged = Database.merge(oldDBFiltered, newDB)
        Json.writeToFile(merged, trg.toFile)
    }
  }
}

def gzipJson(root: Path): Unit = {
  val target = root.resolveSibling(root.getFileName.toString + ".gz")
  if (!Files.exists(target)) {
    println(s"Compressing $root to $target")
    val db = Json.fromFile(root.toFile)
    Json.writeToFile(db, target.toFile)
    println(s"Deleting $root")
    Files.delete(root)
  } else {
    println(s"Warning: both $root and $target exist, will not do anything")
  }
}

vars.phases = Seq(
  new ComputeMinimal("min"),
  CompareMinimal,
  new Consolidate("minimal-min"),
  new ComputeMinimal("more-d"),
  new Consolidate("minimal-more-d"),
  new ComputeMinimal("more-n"),
  new Consolidate("minimal-more-n")
)
