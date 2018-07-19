package ru.ifmo.nds.jmh;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Timeout(time = 10)
@Warmup(time = 1, iterations = 10)
@Measurement(time = 1, iterations = 3)
@Fork(value = 3)
public class VEBPrevBenchmark {
    private int[] left, right;

    @Setup
    public void initialize() {
        int n = 1000;
        left = new int[n];
        right = new int[n];
        Random random = new Random(72357426);
        for (int i = 0; i < n; ++i) {
            left[i] = random.nextInt(2000000000);
            right[i] = random.nextInt(2000000000);
        }
    }

    private int standardPrev(int index, int min) {
        if (index <= min) {
            return index == min ? index : -1;
        }
        return 239;
    }

    private int bitwisePrev(int index, int min) {
        if (index <= min) {
            return min | ((index - min) >> 31);
        }
        return 239;
    }

    @Benchmark
    public int testStandard() {
        int rv = 0;
        int length = left.length;
        for (int i = 0; i < length; ++i) {
            rv += standardPrev(left[i], right[i]);
        }
        return rv;
    }

    @Benchmark
    public int testBitwise() {
        int rv = 0;
        int length = left.length;
        for (int i = 0; i < length; ++i) {
            rv += bitwisePrev(left[i], right[i]);
        }
        return rv;
    }
}
