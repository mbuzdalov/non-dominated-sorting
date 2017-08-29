package ru.ifmo.nds.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import oshi.SystemInfo;

import ru.ifmo.nds.IdCollection;
import ru.ifmo.nds.jmh.JMHBenchmark;
import ru.ifmo.nds.rundb.Record;
import ru.ifmo.nds.rundb.Records;

public final class Benchmark extends JCommanderRunnable {
    private Benchmark() {}

    public static class PositiveIntegerValidator implements IValueValidator<Integer> {
        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value <= 0) {
                throw new ParameterException("Value for '" + name + "' must be positive, you specified " + value + ".");
            }
        }
    }

    public static class ToleranceValidator implements IValueValidator<Double> {
        @Override
        public void validate(String name, Double value) throws ParameterException {
            if (value <= 0 || value >= 1) {
                throw new ParameterException("Value for '" + name + "' must be in (0; 1), you specified " + value + ".");
            }
        }
    }

    @Parameter(names = "--algorithmId",
            required = true,
            description = "Specify the algorithm ID to benchmark.")
    private String algorithmId;

    @Parameter(names = "--forks",
            required = true,
            description = "Specify the number of forks (different JVM instances) to be used.",
            validateValueWith = PositiveIntegerValidator.class)
    private Integer forks;

    @Parameter(names = "--tolerance",
            required = true,
            description = "Specify the tolerance used to determine warm-up iteration count, should be in (0; 1).",
            validateValueWith = ToleranceValidator.class)
    private Double tolerance;

    @Parameter(names = "--plateau",
            required = true,
            description = "Specify the plateau size used to determine warm-up iteration count, should be positive.",
            validateValueWith = PositiveIntegerValidator.class)
    private Integer plateauSize;

    @Parameter(names = "--author",
            required = true,
            description = "Specify the author of the measurement.")
    private String author;

    @Parameter(names = "--comment",
            required = true,
            description = "Specify the comment to the measurement.")
    private String comment;

    @Parameter(names = "--output",
            required = true,
            description = "Specify the output file name.")
    private String outputFileName;

    @Parameter(names = "--append", description = "Append to the output file instead of overwriting it.")
    private boolean shouldAppendToOutput;

    private static final String[] jmhIds = {
            "uniform.hypercube.n10.d2", "uniform.hypercube.n10.d3", "uniform.hypercube.n10.d4",
            "uniform.hypercube.n10.d5", "uniform.hypercube.n10.d6", "uniform.hypercube.n10.d7",
            "uniform.hypercube.n10.d8", "uniform.hypercube.n10.d9", "uniform.hypercube.n10.d10",
            "uniform.hypercube.n100.d2", "uniform.hypercube.n100.d3", "uniform.hypercube.n100.d4",
            "uniform.hypercube.n100.d5", "uniform.hypercube.n100.d6", "uniform.hypercube.n100.d7",
            "uniform.hypercube.n100.d8", "uniform.hypercube.n100.d9", "uniform.hypercube.n100.d10",
            "uniform.hypercube.n1000.d2", "uniform.hypercube.n1000.d3", "uniform.hypercube.n1000.d4",
            "uniform.hypercube.n1000.d5", "uniform.hypercube.n1000.d6", "uniform.hypercube.n1000.d7",
            "uniform.hypercube.n1000.d8", "uniform.hypercube.n1000.d9", "uniform.hypercube.n1000.d10",
            "uniform.hypercube.n10000.d2", "uniform.hypercube.n10000.d3", "uniform.hypercube.n10000.d4",
            "uniform.hypercube.n10000.d5", "uniform.hypercube.n10000.d6", "uniform.hypercube.n10000.d7",
            "uniform.hypercube.n10000.d8", "uniform.hypercube.n10000.d9", "uniform.hypercube.n10000.d10",

            "uniform.hyperplanes.n10.d2.f1", "uniform.hyperplanes.n10.d3.f1", "uniform.hyperplanes.n10.d4.f1",
            "uniform.hyperplanes.n10.d5.f1", "uniform.hyperplanes.n10.d6.f1", "uniform.hyperplanes.n10.d7.f1",
            "uniform.hyperplanes.n10.d8.f1", "uniform.hyperplanes.n10.d9.f1", "uniform.hyperplanes.n10.d10.f1",
            "uniform.hyperplanes.n100.d2.f1", "uniform.hyperplanes.n100.d3.f1", "uniform.hyperplanes.n100.d4.f1",
            "uniform.hyperplanes.n100.d5.f1", "uniform.hyperplanes.n100.d6.f1", "uniform.hyperplanes.n100.d7.f1",
            "uniform.hyperplanes.n100.d8.f1", "uniform.hyperplanes.n100.d9.f1", "uniform.hyperplanes.n100.d10.f1",
            "uniform.hyperplanes.n1000.d2.f1", "uniform.hyperplanes.n1000.d3.f1", "uniform.hyperplanes.n1000.d4.f1",
            "uniform.hyperplanes.n1000.d5.f1", "uniform.hyperplanes.n1000.d6.f1", "uniform.hyperplanes.n1000.d7.f1",
            "uniform.hyperplanes.n1000.d8.f1", "uniform.hyperplanes.n1000.d9.f1", "uniform.hyperplanes.n1000.d10.f1",
            "uniform.hyperplanes.n10000.d2.f1", "uniform.hyperplanes.n10000.d3.f1", "uniform.hyperplanes.n10000.d4.f1",
            "uniform.hyperplanes.n10000.d5.f1", "uniform.hyperplanes.n10000.d6.f1", "uniform.hyperplanes.n10000.d7.f1",
            "uniform.hyperplanes.n10000.d8.f1", "uniform.hyperplanes.n10000.d9.f1", "uniform.hyperplanes.n10000.d10.f1"
    };

    private List<Double> getTimes(String algorithmId,
                                  String datasetId,
                                  int warmUps,
                                  int measurements) throws CLIWrapperException, RunnerException {
        Options options = new OptionsBuilder()
                .forks(1)
                .measurementIterations(measurements)
                .warmupIterations(warmUps)
                .param("datasetId", datasetId)
                .param("algorithmId", algorithmId)
                .include(JMHBenchmark.class.getName())
                .build();

        Collection<RunResult> results = new Runner(options).run();
        List<Double> times = new ArrayList<>();
        for (RunResult result : results) {
            for (BenchmarkResult benchmarkResult : result.getBenchmarkResults()) {
                BenchmarkParams params = benchmarkResult.getParams();
                String myDatasetId = params.getParam("datasetId");
                if (!datasetId.equals(myDatasetId)) {
                    throw new CLIWrapperException("Unable to dig through JMH output: Value for 'datasetId' parameter is '"
                            + myDatasetId + "' but expected '" + datasetId + "'", null);
                }
                String myAlgorithmId = params.getParam("algorithmId");
                if (!algorithmId.equals(myAlgorithmId)) {
                    throw new CLIWrapperException("Unable to dig through JMH output: Value for 'algorithmId' parameter is '"
                            + myAlgorithmId + "' but expected '" + algorithmId + "'", null);
                }
                int count = 0;
                for (IterationResult iterationResult : benchmarkResult.getIterationResults()) {
                    for (Result<?> primary : iterationResult.getRawPrimaryResults()) {
                        if (primary.getStatistics().getN() != 1) {
                            throw new CLIWrapperException("Unable to dig through JMH output: getN() != 1", null);
                        }
                        if (!primary.getScoreUnit().equals("us/op")) {
                            throw new CLIWrapperException("Unable to dig through JMH output: getScoreUnit() = " + primary.getScoreUnit(), null);
                        }
                        double value = primary.getScore() / 1e6;
                        times.add(value / IdCollection.getDataset(datasetId).getNumberOfInstances());
                        ++count;
                    }
                }
                if (count != measurements) {
                    throw new CLIWrapperException("Unable to dig through JMH output: Expected "
                            + measurements + " measurements, found " + count, null);
                }
            }
        }
        return times;
    }

    private int getWarmUpLength(List<Double> times) {
        double expected = times.get(times.size() - 1);
        int lastCount = 0;
        for (int i = times.size() - 1; i >= 0; --i) {
            double curr = times.get(i);
            if (Math.abs(curr - expected) <= tolerance * Math.min(curr, expected)) {
                ++lastCount;
            } else {
                break;
            }
        }
        if (lastCount >= plateauSize) {
            return times.size() - lastCount + plateauSize;
        } else {
            return -1;
        }
    }

    @Override
    protected void run() throws CLIWrapperException {
        try {
            Path output = Paths.get(outputFileName);
            List<Record> allBenchmarks;
            if (shouldAppendToOutput && Files.exists(output) && Files.size(output) > 0) {
                allBenchmarks = Records.loadFromFile(output);
            } else {
                allBenchmarks = new ArrayList<>();
                Files.write(output, Collections.emptyList());
            }

            String javaRuntimeVersion = System.getProperty("java.runtime.version");
            String cpuModel = new SystemInfo().getHardware().getProcessor().getName();

            System.out.println("[info] Java runtime version: '" + javaRuntimeVersion + "'.");
            System.out.println("[info] CPU model: '" + cpuModel + "'.");

            List<Record> records = new ArrayList<>();

            for (String datasetId : jmhIds) {
                System.out.println();
                System.out.println("**************************************************************");
                System.out.println("* Algorithm: " + algorithmId + ", dataset: " + datasetId);
                System.out.println("**************************************************************");
                int warmUpGuess = 5;
                List<Record> localRecords = new ArrayList<>();
                do {
                    int warmUpLength;
                    double bestValue;
                    System.out.println();
                    System.out.println("************* Finding the right warm-up length *************");
                    System.out.println();
                    do {
                        warmUpGuess *= 2;
                        List<Double> times = getTimes(algorithmId, datasetId, 0, warmUpGuess);
                        warmUpLength = getWarmUpLength(times);
                        bestValue = times.get(times.size() - 1);
                        if (warmUpLength == -1) {
                            System.out.println("[warning] " + warmUpGuess
                                    + " iterations is not enough to find a plateau of size " + plateauSize
                                    + " with tolerance " + tolerance + ", doubling...");
                        }
                    } while (warmUpLength == -1);

                    System.out.println();
                    System.out.println("************* Warm-up length is " + warmUpLength + " *************");
                    System.out.println();

                    for (int i = 0; i < forks; ++i) {
                        List<Double> times = getTimes(algorithmId, datasetId, warmUpLength, 1);
                        LocalDateTime measurementTime = LocalDateTime.now();

                        double max = times.stream().mapToDouble(Double::doubleValue).max().orElse(Double.NaN);
                        if (max > (1 + tolerance * 4) * bestValue) {
                            System.out.println("[warning] something is going wrong, max value " + max
                                    + " is much worse than best pre-warm-up value " + bestValue + ". Repeating...");
                            localRecords.clear();
                            break;
                        }

                        localRecords.add(new Record(
                                algorithmId, datasetId, "JMH",
                                author, measurementTime, cpuModel, javaRuntimeVersion, times, comment
                        ));
                    }
                } while (localRecords.size() == 0);
                records.addAll(localRecords);
            }

            allBenchmarks.addAll(records);
            Records.saveToFile(allBenchmarks, output);
        } catch (RunnerException ex) {
            throw new CLIWrapperException("Error while running JMH tests.", ex);
        } catch (IOException ex) {
            throw new CLIWrapperException("Error writing results to output file.", ex);
        }
    }

    public static void main(String[] args) {
        JCommanderRunnable.run(new Benchmark(), args);
    }
}
