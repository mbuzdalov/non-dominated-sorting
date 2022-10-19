package ru.ifmo.nds.jmh.internal;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

import ru.ifmo.nds.util.median.*;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Timeout(time = 100)
@Warmup(time = 1, iterations = 10)
@Measurement(time = 1, iterations = 3)
@Fork(value = 3)
public class MedianBenchmark {
    private static final int ITERATIONS = 1000;

    @Param(value = {
            "2", "5", "10",
            "20", "50", "100",
            "200", "500", "1000",
            "2000", "5000", "10000",
            "20000", "50000", "100000"})
    private int size;

    @Param(value = {"whole-range", "hypercube", "discrete"})
    private String type;

    @Param(value = {"HoareBidirectionalScan", "SwappingSingleScanV1"})
    private String algorithm;

    private double[][] data;
    private double[] temp;
    private DestructiveMedianAlgorithm medianAlgorithm;

    @Setup
    public void initialize() {
        Random random = new Random(size * 723525217L);
        temp = new double[size];
        data = new double[ITERATIONS][size];
        switch (type) {
            case "whole-range":
                for (int i = 0; i < ITERATIONS; ++i) {
                    for (int j = 0; j < size; ++j) {
                        double v;
                        do {
                            v = Double.longBitsToDouble(random.nextLong());
                        } while (Double.isInfinite(v));
                        data[i][j] = v;
                    }
                }
            case "hypercube":
                for (int i = 0; i < ITERATIONS; ++i) {
                    for (int j = 0; j < size; ++j) {
                        data[i][j] = random.nextDouble();
                    }
                }
                break;
            case "discrete":
                for (int i = 0; i < ITERATIONS; ++i) {
                    for (int j = 0; j < size; ++j) {
                        data[i][j] = random.nextInt(100);
                    }
                }
                break;
            default:
                throw new AssertionError("Unknown data type: '" + type + "'");
        }
        DestructiveMedianFactory factory;
        switch (algorithm) {
            case "HoareBidirectionalScan":
                factory = HoareBidirectionalScan.instance();
                break;
            case "SwappingSingleScanV1":
                factory = SwappingSingleScanV1.instance();
                break;
            default:
                throw new AssertionError("Unknown algorithm: '" + algorithm + "'");
        }
        medianAlgorithm = factory.createInstance(size);
    }


    @Benchmark
    public void quickSelect(Blackhole bh) {
        for (double[] test : data) {
            System.arraycopy(test, 0, temp, 0, test.length);
            bh.consume(medianAlgorithm.solve(temp, 0, temp.length));
        }
    }
}
