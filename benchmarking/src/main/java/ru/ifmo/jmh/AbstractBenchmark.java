package ru.ifmo.jmh;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

import ru.ifmo.NonDominatedSorting;
import ru.ifmo.NonDominatedSortingFactory;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public abstract class AbstractBenchmark {
    @Param({"0", "1", "2", "3"})
    @SuppressWarnings({"unused"})
    private int instance;

    @Param({"2", "3", "4", "5", "6", "7", "8", "9", "10"})
    private int dimension;

    @Param({"10", "100", "1000", "10000", "100000"})
    private int numPoints;

    private NonDominatedSorting sorting;
    private double[][] cloudPoints;
    private int[] ranks;

    @Setup
    public void initializeSorterAndData() {
        Random random = new Random();
        sorting = getFactory().getInstance(numPoints, dimension);
        ranks = new int[numPoints];

        cloudPoints = new double[numPoints][dimension];
        for (double[] point : cloudPoints) {
            for (int i = 0; i < dimension; ++i) {
                point[i] = random.nextDouble();
            }
        }
    }

    @TearDown
    public void destroySorter() {
        sorting.close();
    }

    @Benchmark
    public int cloudBenchmark() {
        Arrays.fill(ranks, 0);
        sorting.sort(cloudPoints, ranks);
        int sum = 0;
        for (int i : ranks) {
            sum += i;
        }
        return sum;
    }

    protected abstract NonDominatedSortingFactory getFactory();
}
