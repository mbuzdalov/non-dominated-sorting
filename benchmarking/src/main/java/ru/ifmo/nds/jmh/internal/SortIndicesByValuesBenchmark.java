package ru.ifmo.nds.jmh.internal;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.ArraySorter;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Timeout(time = 10)
@Warmup(time = 1, iterations = 6)
@Measurement(time = 1, iterations = 2)
@Fork(value = 5)
public class SortIndicesByValuesBenchmark {
    @Param({"1", "2", "3", "4", "5", "7", "10", "13", "17", "23",
            "31", "42", "56", "74", "100", "133", "177", "237", "316"})
    private int size;

    private int[][] data;
    private int[] indices;

    @Setup
    public void initialize() {
        int nInstances = 100;
        Random random = new Random(size * 318325462111L);
        data = new int[nInstances][size];
        for (int i = 0; i < nInstances; ++i) {
            for (int j = 0; j < size; ++j) {
                data[i][j] = random.nextInt();
            }
        }
        indices = new int[size];
    }

    @OperationsPerInvocation(100)
    @Benchmark
    public void run(Blackhole bh) {
        for (int[] instance : data) {
            ArrayHelper.fillIdentity(indices, size);
            ArraySorter.sortIndicesByValues(indices, instance, 0, size);
        }
    }
}
