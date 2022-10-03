package ru.ifmo.nds.jmh.internal;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import ru.ifmo.nds.util.ArrayHelper;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Timeout(time = 10)
@Warmup(time = 1, iterations = 10)
@Measurement(time = 1, iterations = 3)
@Fork(value = 3)
public class MedianBenchmark {
    @Param(value = {
            "2", "5", "10",
            "20", "50", "100",
            "200", "500", "1000",
            "2000", "5000", "10000",
            "20000", "50000", "100000"})
    private int size;

    @Param(value = {"hypercube", "discrete"})
    private String type;

    private double[][] data;
    private double[] temp;

    @Setup
    public void initialize() {
        Random random = new Random(size * 723525217L);
        data = new double[10][size];
        switch (type) {
            case "hypercube":
                for (int i = 0; i < 10; ++i) {
                    for (int j = 0; j < size; ++j) {
                        data[i][j] = random.nextDouble();
                    }
                }
                break;
            case "discrete":
                for (int i = 0; i < 10; ++i) {
                    for (int j = 0; j < size; ++j) {
                        data[i][j] = random.nextInt(100);
                    }
                }
                break;
            default:
                throw new AssertionError("Unknown data type: '" + type + "'");
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
    public void quickSelect(Blackhole bh) {
        for (double[] test : data) {
            System.arraycopy(test, 0, temp, 0, test.length);
            bh.consume(ArrayHelper.destructiveMedian(temp, 0, temp.length));
        }
    }
}
