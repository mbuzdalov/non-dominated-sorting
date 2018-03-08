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
import java.util.function.Predicate;

import com.beust.jcommander.IValueValidator;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.profile.CompilerProfiler;
import org.openjdk.jmh.profile.GCProfiler;
import org.openjdk.jmh.profile.HotspotCompilationProfiler;
import org.openjdk.jmh.results.BenchmarkResult;
import org.openjdk.jmh.results.IterationResult;
import org.openjdk.jmh.results.Result;
import org.openjdk.jmh.results.RunResult;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
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

    @Parameter(names = "--algorithmId",
            required = true,
            description = "Specify the algorithm ID to benchmark.")
    private String algorithmId;

    @Parameter(names = "--forks",
            required = true,
            description = "Specify the number of forks (different JVM instances) to be used.",
            validateValueWith = PositiveIntegerValidator.class)
    private Integer forks;

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

    @Parameter(names = "--only-list", description = "Only list datasets to be tested")
    private boolean onlyList;

    @Parameter(names = "--enable-compiler-profiler", description = "Enable the CompilerProfiler from JMH")
    private boolean enableCompilerProfiler;

    @Parameter(names = "--enable-hotspot-compilation-profiler", description = "Enable the HotspotCompilationProfiler from JMH")
    private boolean enableHotspotCompilationProfiler;

    @Parameter(names = "--enable-gc-profiler", description = "Enable the GCProfiler from JMH")
    private boolean enableGCProfiler;

    @Parameter(names = "--remove",
            variableArity = true,
            description = "Specify which dataset IDs to remove.",
            converter = DatasetFilterStringConverter.class)
    private List<Predicate<String>> removeFilters;

    @Parameter(names = "--retain",
            variableArity = true,
            description = "Specify which dataset IDs to retain.",
            converter = DatasetFilterStringConverter.class)
    private List<Predicate<String>> retainFilters;

    private List<Double> getTimes(String algorithmId,
                                  String datasetId) throws CLIWrapperException, RunnerException {
        ChainedOptionsBuilder builder = new OptionsBuilder()
                .forks(1)
                .measurementIterations(1)
                .measurementTime(TimeValue.seconds(1))
                .warmupIterations(1)
                .warmupTime(TimeValue.seconds(6))
                .param("datasetId", datasetId)
                .param("algorithmId", algorithmId)
                .include(JMHBenchmark.class.getName());

        if (enableCompilerProfiler) {
            builder = builder.addProfiler(CompilerProfiler.class);
        }
        if (enableGCProfiler) {
            builder = builder.addProfiler(GCProfiler.class);
        }
        if (enableHotspotCompilationProfiler) {
            builder = builder.addProfiler(HotspotCompilationProfiler.class);
        }

        Options options = builder.build();
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
                if (count != 1) {
                    throw new CLIWrapperException(
                            "Unable to dig through JMH output: Expected one measurement, found " + count, null);
                }
            }
        }
        return times;
    }

    @Override
    protected void run() throws CLIWrapperException {
        List<String> datasets = new ArrayList<>(IdCollection.getAllDatasetIds());
        if (removeFilters != null) {
            for (Predicate<String> removeFilter : removeFilters) {
                datasets.removeIf(removeFilter);
            }
        }
        if (retainFilters != null) {
            for (Predicate<String> retainFilter : retainFilters) {
                datasets.removeIf(retainFilter.negate());
            }
        }
        if (onlyList) {
            System.out.println("[info] These dataset IDs would have been used:");
            for (String s : datasets) {
                System.out.println("[info]   " + s);
            }
            return;
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

            String javaRuntimeVersion = System.getProperty("java.runtime.version");
            String cpuModel = new SystemInfo().getHardware().getProcessor().getName();

            System.out.println("[info] Java runtime version: '" + javaRuntimeVersion + "'.");
            System.out.println("[info] CPU model: '" + cpuModel + "'.");

            List<Record> records = new ArrayList<>();

            for (String datasetId : datasets) {
                System.out.println();
                System.out.println("**************************************************************");
                System.out.println("* Algorithm: " + algorithmId + ", dataset: " + datasetId);
                System.out.println("**************************************************************");
                List<Record> localRecords = new ArrayList<>();

                for (int i = 0; i < forks; ++i) {
                    List<Double> times = getTimes(algorithmId, datasetId);
                    LocalDateTime measurementTime = LocalDateTime.now();

                    localRecords.add(new Record(
                            algorithmId, datasetId, "JMH",
                            author, measurementTime, cpuModel, javaRuntimeVersion, times, comment
                    ));
                }
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
