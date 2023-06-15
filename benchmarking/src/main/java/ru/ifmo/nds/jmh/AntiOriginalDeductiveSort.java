package ru.ifmo.nds.jmh;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import ru.ifmo.nds.IdCollection;
import ru.ifmo.nds.NonDominatedSorting;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.SECONDS)
@Timeout(time = 1, timeUnit = TimeUnit.HOURS)
@Warmup(time = 6, iterations = 1)
@Measurement(time = 1, iterations = 1)
@Fork(5)
public class AntiOriginalDeductiveSort {
    @Param("The algorithm should be set explicitly")
    private String algorithmId;
    private NonDominatedSorting sorting;
    private double[][] dataset;
    private int[] ranks;

    @Param({"10", "100", "1000", "10000"})
    private int n;

    @Param({"2", "3", "5", "10"})
    private int d;

    @Setup
    public void initializeSorterAndData() {
        int h = this.n / 2;
        int n = h * 2;
        sorting = IdCollection.getNonDominatedSortingFactory(algorithmId).getInstance(n, d);
        ranks = new int[n];
        dataset = new double[n][d];

        for (int i = 0; i < h; ++i) {
            dataset[i][d - 2] = i + 1;
            dataset[i][d - 1] = h - i - 1;
            dataset[h + i][d - 1] = n - i;
        }
    }

    @Benchmark
    public void benchmarkCall(Blackhole blackhole) {
        Arrays.fill(ranks, 0);
        sorting.sort(dataset, ranks);
        blackhole.consume(ranks);
    }

    @TearDown
    public void destroySorter() {
        sorting.close();
    }
}
