import java.nio.file.{Files, Path}

import ru.ifmo.ds.Database
import ru.ifmo.ds.io.Json
import ru.ifmo.ds.ops.FindDifferences

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

class ComputeMinimal(useKey: String) extends Phase(s"phase.minimal-$useKey.compute") {
  override def execute(projectRoot: Path, curr: Path, prev: Option[Path]): Unit = {
    val currentPhaseOut = s"minimal-$useKey.json"
    val rawDir = curr.resolve(DataSubdirectoryRaw)
    Files.createDirectories(rawDir)
    val outputFile = rawDir.resolve(currentPhaseOut)
    val listOfAlgorithms = curr.resolve(ListOfAlgorithms)
    val algorithms = if (Files.exists(listOfAlgorithms)) {
      Utils.firstTokensOfUncommentedLines(listOfAlgorithms).mkString("--algo=", ",", "")
    } else ""
    if (algorithms == "--algo=") {
      // When an empty parameter list is given, JMH thinks one shall use the compiled-in parameters, which fails.
      // Write an empty JSON file instead.
      Utils.writeLines(outputFile, Seq("[]"))
    } else {
      Utils.runProcess(projectRoot,
        "sbt",
        "project benchmarking",
        s"jmh:runMain ru.ifmo.nds.jmh.main.Minimal $algorithms --use=$useKey --out=${outputFile.toAbsolutePath}")
    }
    Utils.gzipJson(outputFile)
  }
}

object CompareMinimal extends Phase("phase.minimal-min.compare") {
  override def execute(projectRoot: Path, curr: Path, prevOption: Option[Path]): Unit = {
    val currentPhaseIn = "minimal-min.json.gz"
    val listOfAlgorithms = curr.resolve(ListOfAlgorithms)
    prevOption match {
      case Some(prev) =>
        val oldDB = Json.fromFile(prev.resolve(DataSubdirectoryConsolidated).resolve(currentPhaseIn).toFile)
        val newDB = Json.fromFile(curr.resolve(DataSubdirectoryRaw).resolve(currentPhaseIn).toFile)
        val commonAlgorithms = oldDB.valuesUnderKey(KeyAlgorithm).intersect(newDB.valuesUnderKey(KeyAlgorithm))
        val listener = new CompareListener(BasicPValue / commonAlgorithms.size, KeyAlgorithm)
        FindDifferences.traverse(oldDB, newDB, KeyAlgorithm +: KeyCats, KeyValue, listener)
        Utils.writeLines(listOfAlgorithms, listener.result())
      case None =>
        // No previous runs detected. Need to write all algorithms to the file
        val file = curr.resolve(DataSubdirectoryRaw).resolve(currentPhaseIn)
        val allAlgorithms = Json.fromFile(file.toFile).valuesUnderKey(KeyAlgorithm).flatten.toIndexedSeq.sorted
        Utils.writeLines(listOfAlgorithms, allAlgorithms)
    }
  }
}

class Consolidate(key: String) extends Phase(s"phase.$key.consolidate") {
  override def execute(projectRoot: Path, curr: Path, prevOption: Option[Path]): Unit = {
    val target = curr.resolve(DataSubdirectoryConsolidated)
    val currentPhaseOut = key + ".json.gz"
    Files.createDirectories(target)
    val trg = target.resolve(currentPhaseOut)
    prevOption match {
      case None =>
        // no previous runs - just copy a file over
        val src = curr.resolve(DataSubdirectoryRaw).resolve(currentPhaseOut)
        if (!Files.exists(trg) || !Files.isSameFile(trg, src)) {
          Files.deleteIfExists(trg)
          Files.createLink(trg, src)
        }
      case Some(prev) =>
        val oldDB = Json.fromFile(prev.resolve(DataSubdirectoryConsolidated).resolve(currentPhaseOut).toFile)
        val newDB = Json.fromFile(curr.resolve(DataSubdirectoryRaw).resolve(currentPhaseOut).toFile)
        val differingAlgorithms = Utils.firstTokensOfUncommentedLines(curr.resolve(ListOfAlgorithms))
        val oldDBFiltered = oldDB.filter(e => e.contains(KeyAlgorithm) && !differingAlgorithms.contains(e(KeyAlgorithm)))
        val merged = Database.merge(oldDBFiltered, newDB)
        Json.writeToFile(merged, trg.toFile)
    }
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
