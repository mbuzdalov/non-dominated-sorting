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
@Fork(value = 3)
public class LexSortBenchmark {
    @Param({"3", "10", "17", "31", "56", "100", "177", "316", "1000", "10000", "100000"})
    private int size;

    @Param({"2", "5", "20"})
    private int dimension;

    private double[][][] data;
    private int[] indices;
    private ArraySorter sorter;

    @Setup
    public void initialize() {
        int nInstances = 10;
        Random random = new Random(size * 318325462111L);
        data = new double[nInstances][size][dimension];
        for (int i = 0; i < nInstances; ++i) {
            for (int j = 0; j < size; ++j) {
                for (int k = 0; k < dimension; ++k) {
                    data[i][j][k] = random.nextDouble();
                }
            }
        }
        indices = new int[size];
        sorter = new ArraySorter(size);
    }

    @Benchmark
    public void run(Blackhole bh) {
        for (double[][] instance : data) {
            ArrayHelper.fillIdentity(indices, size);
            sorter.lexicographicalSort(instance, indices, 0, size, dimension - 1);
        }
    }
}
