package ru.ifmo.nds.jmh;

import org.openjdk.jmh.annotations.*;
import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.NonDominatedSortingFactory;
import ru.ifmo.nds.rundb.AllDatasets;
import ru.ifmo.nds.rundb.Dataset;

import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@Fork(3)
@Warmup(iterations = 5)
@Measurement(iterations = 5)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public abstract class AbstractBenchmark {
    private NonDominatedSorting sorting;
    private Dataset dataset;

    @Param({
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
            "uniform.hyperplanes.n10000.d8.f1", "uniform.hyperplanes.n10000.d9.f1", "uniform.hyperplanes.n10000.d10.f1",
    })
    private String datasetId;
    
    @Setup
    public void initializeSorterAndData() {
        dataset = AllDatasets.getDataset(datasetId);
        sorting = getFactory().getInstance(dataset.getNumberOfPoints(), dataset.getDimension());
    }

    private int runBenchmark() {
        return dataset.runAlgorithm(sorting, dataset.getNumberOfPoints());
    }

    @TearDown
    public void destroySorter() {
        sorting.close();
    }

    @Benchmark
    public int benchmarkCall() {
        return runBenchmark();
    }

    public abstract NonDominatedSortingFactory getFactory();
}
