package ru.ifmo.nds.jmh.main;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import ru.ifmo.nds.IdCollection;
import ru.ifmo.nds.NonDominatedSorting;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Timeout(time = 1, timeUnit = TimeUnit.HOURS)
@Warmup(time = 6, iterations = 1)
@Measurement(time = 1, iterations = 1)
@Fork(11)
public class WorstCaseCollection {
    private static final int INSTANCES = 9;

    @Param("The algorithm should be set explicitly")
    private String algorithmId;
    private NonDominatedSorting sorting;
    private double[][][] dataset;
    private int[] ranks;

    @Param({"10", "100", "1000"})
    private int n;

    @Param({"2", "3", "5", "10"})
    private int d;

    @Setup
    public void initializeSorterAndData() {
        sorting = IdCollection.getNonDominatedSortingFactory(algorithmId).getInstance(n, d);
        ranks = new int[n];
        dataset = new double[INSTANCES][n][d];
        Random random = new Random(Arrays.hashCode(new int[] {n, d}));

        fillCorrelated(random, dataset[0], 0);
        fillCorrelated(random, dataset[1], 1);
        fillCorrelated(random, dataset[2], d - 1);
        fillMultipleLayers(random, dataset[3]);
        fillMultipleLayers(random, dataset[4]);
        fillMultipleLayers(random, dataset[5]);
        fillUniform(random, dataset[6]);
        fillUniform(random, dataset[7]);
        fillUniform(random, dataset[8]);
    }

    private void fillUniform(Random random, double[][] instance) {
        for (int i = 0; i < n; ++i) {
            for (int j = 0; j < d; ++j) {
                instance[i][j] = random.nextDouble();
            }
        }
    }

    private void fillMultipleLayers(Random random, double[][] instance) {
        int points = n;
        int fronts = n / 3;
        int pointsInLayer = (points + fronts - 1) / fronts;
        for (int i = 0; i < pointsInLayer; ++i) {
            for (int j = 1; j < d; ++j) {
                instance[i][j] = random.nextDouble();
                instance[i][0] -= instance[i][j];
            }
            instance[i][0] += 0.5 * d;
        }
        for (int i = pointsInLayer; i < points; ++i) {
            System.arraycopy(instance[i - pointsInLayer], 0, instance[i], 0, d);
            for (int j = 0; j < d; ++j) {
                instance[i][j] += 1e-8;
            }
        }
        Collections.shuffle(Arrays.asList(instance), random);
    }

    private void fillCorrelated(Random random, double[][] instance, int x) {
        for (int i = 0; i < n; ++i) {
            double first = random.nextDouble();
            for (int k = 0; k < d; ++k) {
                instance[i][k] = k == x ? -first : first;
            }
        }
        Collections.shuffle(Arrays.asList(instance), random);
    }

    @OperationsPerInvocation(INSTANCES)
    @Benchmark
    public void benchmarkCall(Blackhole blackhole) {
        for (double[][] instance : dataset) {
            Arrays.fill(ranks, 0);
            sorting.sort(instance, ranks);
            blackhole.consume(ranks);
        }
    }

    @TearDown
    public void destroySorter() {
        sorting.close();
    }
}
