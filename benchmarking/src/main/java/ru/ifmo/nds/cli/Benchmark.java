package ru.ifmo.nds.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

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
import ru.ifmo.nds.simple.SimpleBenchmark;

public final class Benchmark extends JCommanderRunnable {
    private Benchmark() {}

    public enum Type {
        JMH, simple
    }

    public static class PositiveIntegerValidator implements IValueValidator<Integer> {
        @Override
        public void validate(String name, Integer value) throws ParameterException {
            if (value <= 0) {
                throw new ParameterException("Value for '" + name + "' must be positive, you specified " + value + ".");
            }
        }
    }

    public static class PositiveDoubleValidator implements IValueValidator<Double> {
        @Override
        public void validate(String name, Double value) throws ParameterException {
            if (value <= 0.0) {
                throw new ParameterException("Value for '" + name + "' must be positive, you specified " + value + ".");
            }
        }
    }

    @Parameter(names = "--type",
            required = true,
            description = "Specify the benchmark type.")
    private Type benchmarkType;

    @Parameter(names = "--algorithmId",
            required = true,
            description = "Specify the algorithm ID to benchmark.")
    private String algorithmId;

    @Parameter(names = "--forks",
            description = "Specify the number of forks (different JVM instances) to be used.",
            validateValueWith = PositiveIntegerValidator.class)
    private Integer forks = 1;

    @Parameter(names = "--measurements",
            required = true,
            description = "Specify the number of measurements for each configuration.",
            validateValueWith = PositiveIntegerValidator.class)
    private Integer measurements;

    @Parameter(names = "--warmup-measurements",
            description = "Specify the number of warmup measurements for each configuration.",
            validateValueWith = PositiveIntegerValidator.class)
    private Integer warmUpMeasurements;

    @Parameter(names = "--required-precision",
            description = "Specify the required precision for --type=simple (> 0.0).",
            validateValueWith = PositiveDoubleValidator.class)
    private double requiredPrecision = 0.02;

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

    private void runJMH() throws CLIWrapperException {
        Options options = new OptionsBuilder()
                .forks(forks)
                .measurementIterations(measurements)
                .warmupIterations(warmUpMeasurements == null ? measurements : warmUpMeasurements)
                .param("datasetId", jmhIds)
                .param("algorithmId", algorithmId)
                .include(JMHBenchmark.class.getName())
                .build();
        try {
            Path output = Paths.get(outputFileName);
            List<Record> allBenchmarks;
            if (shouldAppendToOutput && Files.exists(output) && Files.size(output) > 0) {
                allBenchmarks = Records.loadFromFile(output);
            } else {
                allBenchmarks = new ArrayList<>();
                Files.write(output, Collections.emptyList());
            }

            Collection<RunResult> results = new Runner(options).run();
            List<Record> records = new ArrayList<>();

            String javaRuntimeVersion = System.getProperty("java.runtime.version");
            String cpuModel = new SystemInfo().getHardware().getProcessor().getName();

            System.out.println("[info] Java runtime version: '" + javaRuntimeVersion + "'.");
            System.out.println("[info] CPU model: '" + cpuModel + "'.");

            for (RunResult result : results) {
                for (BenchmarkResult benchmarkResult : result.getBenchmarkResults()) {
                    BenchmarkParams params = benchmarkResult.getParams();
                    String datasetId = params.getParam("datasetId");
                    if (datasetId == null) {
                        throw new CLIWrapperException("Unable to dig through JMH output: No 'datasetId' parameter", null);
                    }
                    String algorithmId = params.getParam("algorithmId");
                    if (algorithmId == null) {
                        throw new CLIWrapperException("Unable to dig through JMH output: No 'algorithmId' parameter", null);
                    }
                    List<Double> times = new ArrayList<>();
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
                        }
                    }
                    long endTimeMillis = benchmarkResult.getMetadata().getStopTime();
                    LocalDateTime measurementTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimeMillis),
                            TimeZone.getDefault().toZoneId());

                    records.add(new Record(
                            algorithmId, datasetId, "JMH",
                            author, measurementTime, cpuModel, javaRuntimeVersion, times, comment
                    ));
                }
            }

            allBenchmarks.addAll(records);
            Records.saveToFile(allBenchmarks, output);
        } catch (RunnerException ex) {
            throw new CLIWrapperException("Error while running JMH tests.", ex);
        } catch (IOException ex) {
            throw new CLIWrapperException("Error writing results to output file.", ex);
        }
    }

    private void runSimple() throws CLIWrapperException {
        if (forks != 1) {
            System.out.println("[warning] Parameter '--forks' is not yet supported when '--type simple'.");
        }
        try {
            Path output = Paths.get(outputFileName);
            List<Record> allBenchmarks;
            if (shouldAppendToOutput && Files.exists(output) && Files.size(output) > 0) {
                allBenchmarks = Records.loadFromFile(output);
            } else {
                allBenchmarks = new ArrayList<>();
                Files.write(output, Collections.emptyList());
            }

            allBenchmarks.addAll(new SimpleBenchmark(
                    algorithmId,
                    Arrays.asList(jmhIds),
                    requiredPrecision).evaluate(author, comment, measurements));

            Records.saveToFile(allBenchmarks, output);
        } catch (IOException ex) {
            throw new CLIWrapperException("Error writing results to output file.", ex);
        }
    }

    @Override
    protected void run() throws CLIWrapperException {
        switch (benchmarkType) {
            case JMH:
                runJMH();
                break;
            case simple:
                runSimple();
                break;
        }
    }

    public static void main(String[] args) {
        JCommanderRunnable.run(new Benchmark(), args);
    }
}
