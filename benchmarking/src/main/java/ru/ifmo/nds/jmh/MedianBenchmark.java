package ru.ifmo.nds.jmh;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;

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
            bh.consume(medianBySimpleQuickSort(test, test.length));
        }
    }

    @Benchmark
    public void centerQuickSort(Blackhole bh) {
        for (double[] test : data) {
            bh.consume(medianByCenterQuickSort(test, test.length));
        }
    }

    private void swap(double[] array, int l, int r) {
        double v = array[l];
        array[l] = array[r];
        array[r] = v;
    }

    private double medianBySimpleQuickSort(double[] array, int until) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int count = 0;
        int from = 0;
        int index = (from + until) >>> 1;
        while (from + 1 < until) {
            double pivot = array[++count > 30 ? random.nextInt(from, until) : (from + until) >>> 1];
            int l = from, r = until - 1;
            while (l <= r) {
                while (array[l] < pivot) ++l;
                while (array[r] > pivot) --r;
                if (l <= r) {
                    swap(array, l++, r--);
                }
            }
            if (index <= r) {
                until = r + 1;
            } else if (l <= index) {
                from = l;
            } else {
                break;
            }
        }
        return array[index];

    }

    private double medianByCenterQuickSort(double[] array, int until) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int count = 0;
        int from = 0;
        int index = (from + until) >>> 1;
        while (from + 1 < until) {
            double pivot = array[++count > 20 ? random.nextInt(from, until) : (from + until) >>> 1];
            int pivotFirst = from, greaterFirst = until - 1;
            for (int i = from; i <= greaterFirst; ++i) {
                double value = array[i];
                if (value == pivot) {
                    continue;
                }
                if (value < pivot) {
                    array[i] = array[pivotFirst];
                    array[pivotFirst++] = value;
                } else {
                    double notGreater = array[greaterFirst];
                    while (notGreater > pivot) {
                        notGreater = array[--greaterFirst];
                    }
                    if (notGreater == pivot) {
                        array[i] = notGreater;
                    } else {
                        array[i] = array[pivotFirst];
                        array[pivotFirst++] = notGreater;
                    }
                    array[greaterFirst--] = value;
                }
            }
            --pivotFirst;
            ++greaterFirst;
            if (index <= pivotFirst) {
                until = pivotFirst + 1;
            } else if (index >= greaterFirst) {
                from = greaterFirst;
            } else {
                break;
            }
        }
        return array[index];
    }
}
