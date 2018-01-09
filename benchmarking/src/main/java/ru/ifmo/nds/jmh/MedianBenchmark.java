package ru.ifmo.nds.jmh;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import ru.ifmo.nds.util.ArrayHelper;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Timeout(time = 10)
@Warmup(iterations = 10)
@Measurement(iterations = 3)
@Fork(value = 3)
public class MedianBenchmark {
    @Param(value = {
            "2", "5", "10",
            "20", "50", "100",
            "200", "500", "1000",
            "2000", "5000", "10000",
            "20000", "50000", "100000",
            "200000", "500000", "1000000"})
    private int size;

    private double[][] data;
    private double[] temp;

    @Setup(Level.Invocation)
    public void initialize() {
        Random random = new Random(size * 723525217);
        data = new double[10][size];
        for (int i = 0; i < 10; ++i) {
            for (int j = 0; j < size; ++j) {
                data[i][j] = random.nextDouble();
            }
        }
        temp = new double[size];
    }

    @Benchmark
    public void baseline(Blackhole bh) {
        for (double[] test : data) {
            System.arraycopy(test, 0, temp, 0, test.length);
            bh.consume(temp);
        }
    }

    @Benchmark
    public void simpleQuickSort(Blackhole bh) {
        for (double[] test : data) {
            bh.consume(ArrayHelper.destructiveMedian(test, 0, test.length));
        }
    }

    @Benchmark
    public void centerQuickSort(Blackhole bh) {
        for (double[] test : data) {
            bh.consume(ArrayHelper.destructiveMedianCenter(test, 0, test.length));
        }
    }
}
