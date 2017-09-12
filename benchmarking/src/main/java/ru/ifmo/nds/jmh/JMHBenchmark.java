package ru.ifmo.nds.jmh;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import ru.ifmo.nds.IdCollection;
import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.rundb.Dataset;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Timeout(time = 1, timeUnit = TimeUnit.HOURS)
public class JMHBenchmark {
    private NonDominatedSorting sorting;
    private Dataset dataset;

    @Param("Please use ru.ifmo.nds.cli.Benchmark to run this JMH benchmark")
    private String datasetId;

    @Param("Please use ru.ifmo.nds.cli.Benchmark to run this JMH benchmark")
    private String algorithmId;

    @Setup
    public void initializeSorterAndData() {
        dataset = IdCollection.getDataset(datasetId);
        sorting = IdCollection.getNonDominatedSortingFactory(algorithmId)
                .getInstance(dataset.getMaxNumberOfPoints(), dataset.getMaxDimension());
    }

    private int runBenchmark() {
        return dataset.runAlgorithm(sorting, dataset.getMaxNumberOfPoints());
    }

    @TearDown
    public void destroySorter() {
        sorting.close();
    }

    @Benchmark
    public int benchmarkCall() {
        return runBenchmark();
    }
}
