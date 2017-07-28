package ru.ifmo.jmh;

import org.openjdk.jmh.annotations.*;
import ru.ifmo.NonDominatedSorting;
import ru.ifmo.NonDominatedSortingFactory;

import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
@Fork(5)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public abstract class AbstractBenchmark {
    @Param({"2", "3", "4", "5", "10"})
    private int dimension;

    private NonDominatedSorting sorting;
    private Dataset uniformHypercube10;
    private Dataset uniformHypercube100;
    private Dataset uniformHypercube1000;
    private Dataset uniformHypercube10000;
    private Dataset uniformHyperplane10;
    private Dataset uniformHyperplane100;
    private Dataset uniformHyperplane1000;
    private Dataset uniformHyperplane10000;

    @Setup
    public void initializeSorterAndData() {
        uniformHypercube10 = Dataset.generateUniformHypercube(10, dimension);
        uniformHypercube100 = Dataset.generateUniformHypercube(100, dimension);
        uniformHypercube1000 = Dataset.generateUniformHypercube(1000, dimension);
        uniformHypercube10000 = Dataset.generateUniformHypercube(10000, dimension);

        uniformHyperplane10 = Dataset.generateUniformHyperplane(10, dimension);
        uniformHyperplane100 = Dataset.generateUniformHyperplane(100, dimension);
        uniformHyperplane1000 = Dataset.generateUniformHyperplane(1000, dimension);
        uniformHyperplane10000 = Dataset.generateUniformHyperplane(10000, dimension);

        sorting = getFactory().getInstance(10000, dimension);
    }

    @TearDown
    public void destroySorter() {
        sorting.close();
    }

    @Benchmark
    @Warmup(iterations = 20, time = 1)
    @Measurement(iterations = 5, time = 1)
    public int uniformHypercube_N10() {
        return uniformHypercube10.runSortingOnMe(sorting);
    }

    @Benchmark
    @Warmup(iterations = 20, time = 1)
    @Measurement(iterations = 5, time = 1)
    public int uniformHypercube_N100() {
        return uniformHypercube100.runSortingOnMe(sorting);
    }

    @Benchmark
    @Warmup(iterations = 20, time = 1)
    @Measurement(iterations = 5, time = 1)
    public int uniformHypercube_N1000() {
        return uniformHypercube1000.runSortingOnMe(sorting);
    }

    @Benchmark
    @Warmup(iterations = 20, time = 10)
    @Measurement(iterations = 5, time = 10)
    public int uniformHypercube_N10000() {
        return uniformHypercube10000.runSortingOnMe(sorting);
    }

    @Benchmark
    @Warmup(iterations = 20, time = 1)
    @Measurement(iterations = 5, time = 1)
    public int uniformHyperplane_N10() {
        return uniformHyperplane10.runSortingOnMe(sorting);
    }

    @Benchmark
    @Warmup(iterations = 20, time = 1)
    @Measurement(iterations = 5, time = 1)
    public int uniformHyperplane_N100() {
        return uniformHyperplane100.runSortingOnMe(sorting);
    }

    @Benchmark
    @Warmup(iterations = 20, time = 1)
    @Measurement(iterations = 5, time = 1)
    public int uniformHyperplane_N1000() {
        return uniformHyperplane1000.runSortingOnMe(sorting);
    }

    @Benchmark
    @Warmup(iterations = 20, time = 10)
    @Measurement(iterations = 5, time = 10)
    public int uniformHyperplane_N10000() {
        return uniformHyperplane10000.runSortingOnMe(sorting);
    }

    protected abstract NonDominatedSortingFactory getFactory();
}
