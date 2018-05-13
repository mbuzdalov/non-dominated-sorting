package ru.ifmo.nds.jmh;

import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Timeout(time = 10)
@Warmup(iterations = 10)
@Measurement(iterations = 3)
@Fork(value = 3)
public class VEBBitwiseBenchmark {
    private int[] masks;
    private int[] tests;

    @Setup
    public void initialize() {
        Random random = new Random(723465423);
        int length = 1000000;
        masks = new int[length];
        tests = new int[length];
        for (int i = 0; i < length; ++i) {
            masks[i] = random.nextInt();
            tests[i] = random.nextInt(32);
        }
    }

    @Benchmark
    public int countMinByTrailingZeros() {
        int rv = 0;
        for (int i = 0, iMax = tests.length; i < iMax; ++i) {
            int l = tests[i], v = masks[i];
            if (l <= Integer.numberOfTrailingZeros(v))  {
                ++rv;
            }
        }
        return rv;
    }

    @Benchmark
    public int countMinByShifts() {
        int rv = 0;
        for (int i = 0, iMax = tests.length; i < iMax; ++i) {
            int l = tests[i], v = masks[i];
            if (((v << ~l) << 1) == 0) {
                ++rv;
            }
        }
        return rv;
    }

    @Benchmark
    public int countMaxByLeadingZeros() {
        int rv = 0;
        for (int i = 0, iMax = tests.length; i < iMax; ++i) {
            int l = tests[i], v = masks[i];
            if (l >= 31 - Integer.numberOfLeadingZeros(v))  {
                ++rv;
            }
        }
        return rv;
    }

    @Benchmark
    public int countMaxByShifts() {
        int rv = 0;
        for (int i = 0, iMax = tests.length; i < iMax; ++i) {
            int l = tests[i], v = masks[i];
            if (((v >>> l) >>> 1) == 0) {
                ++rv;
            }
        }
        return rv;
    }
}
