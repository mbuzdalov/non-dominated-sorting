package ru.ifmo.jmh;

import org.openjdk.jmh.annotations.*;

import ru.ifmo.NonDominatedSorting;
import ru.ifmo.NonDominatedSortingFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@Fork(5)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public abstract class AbstractBenchmark {
    @Param({"0", "1", "2", "3", "4"})
    @SuppressWarnings({"unused"})
    private int instance;

    @Param({"2", "3", "4", "6", "8", "10", "15", "20"})
    private int dimension;

    private NonDominatedSorting sorting;
    private List<Dataset> datasets = new ArrayList<>();

    @Setup
    public void initializeSorterAndData() {
        datasets.clear();
        datasets.add(Dataset.generateUniformHypercube(10, dimension));
        datasets.add(Dataset.generateUniformHypercube(100, dimension));
        datasets.add(Dataset.generateUniformHypercube(1000, dimension));
        datasets.add(Dataset.generateUniformHypercube(10000, dimension));
        sorting = getFactory().getInstance(10000, dimension);
    }

    @TearDown
    public void destroySorter() {
        sorting.close();
    }

    @Benchmark
    @Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 5, time = 1)
    public int cloudBenchmark_N10() {
        return datasets.get(0).runSortingOnMe(sorting);
    }

    @Benchmark
    @Warmup(iterations = 5, time = 100, timeUnit = TimeUnit.MILLISECONDS)
    @Measurement(iterations = 5, time = 1)
    public int cloudBenchmark_N100() {
        return datasets.get(1).runSortingOnMe(sorting);
    }

    @Benchmark
    @Warmup(iterations = 5, time = 1)
    @Measurement(iterations = 5, time = 1)
    public int cloudBenchmark_N1000() {
        return datasets.get(2).runSortingOnMe(sorting);
    }

    @Benchmark
    @Warmup(iterations = 5, time = 10)
    @Measurement(iterations = 5, time = 10)
    public int cloudBenchmark_N10000() {
        return datasets.get(3).runSortingOnMe(sorting);
    }

    protected abstract NonDominatedSortingFactory getFactory();
}
