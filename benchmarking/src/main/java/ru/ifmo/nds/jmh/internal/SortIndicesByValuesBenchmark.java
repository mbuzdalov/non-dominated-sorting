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
    @Param({"1", "2", "3", "4", "5", "7", "10", "13", "17", "23", "31", "42", "56", "74", "100", "133", "177", "237", "316"})
    private int size;

    @Param({"0",
            "10", "12", "14", "16", "18", "20", "22", "24", "26", "28", "30", "32", "34", "36", "38",
            "40", "42", "44", "46", "48", "50", "52", "54", "56", "58", "60", "62", "64", "66", "68",
            "70", "72", "74", "76", "78", "80", "82", "84", "86", "88", "90", "92", "94", "96", "98",
            "100", "102", "104", "108", "112", "116", "120", "124", "128", "132", "136", "140", "144",
            "148", "152", "156", "160", "164", "168", "172", "176", "180", "184", "188", "196", "200"})
    private int threshold;

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
        if (threshold == 0) {
            for (int[] instance : data) {
                ArrayHelper.fillIdentity(indices, size);
                ArraySorter.sortIndicesByValues(indices, instance, 0, size);
            }
        } else {
            for (int[] instance : data) {
                ArrayHelper.fillIdentity(indices, size);
                sortIndicesByValues(indices, instance, 0, size);
            }
        }
    }

    private static long splitIndicesByRanks(int[] indices, int[] values, int from, int until) {
        int left = from, right = until - 1;
        int pivot = values[indices[(from + until) >>> 1]];
        int sl, sr;
        while (left <= right) {
            while (values[sl = indices[left]] < pivot) ++left;
            while (values[sr = indices[right]] > pivot) --right;
            if (left <= right) {
                indices[left] = sr;
                indices[right] = sl;
                ++left;
                --right;
            }
        }
        return ((long) (right) << 32) ^ left; // left is non-negative
    }

    private static void insertionSortIndicesByValues(int[] indices, int[] values, int from, int to) {
        for (int i = from, j = i; i < to; j = i) {
            int ii = indices[++i], ij;
            int ai = values[ii];
            while (ai < values[ij = indices[j]]) {
                indices[j + 1] = ij;
                if (--j < from) {
                    break;
                }
            }
            indices[j + 1] = ii;
        }
    }

    private void sortIndicesByValues(int[] indices, int[] values, int from, int until) {
        if (from + threshold > until) {
            insertionSortIndicesByValues(indices, values, from, until - 1);
        } else {
            long pack = splitIndicesByRanks(indices, values, from, until);
            int left = (int) pack;
            int right = (int) (pack >> 32);
            if (from < right) {
                sortIndicesByValues(indices, values, from, right + 1);
            }
            if (left + 1 < until) {
                sortIndicesByValues(indices, values, left, until);
            }
        }
    }
}
