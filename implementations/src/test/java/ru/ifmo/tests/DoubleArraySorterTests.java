package ru.ifmo.tests;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;
import ru.ifmo.util.DoubleArraySorter;

public class DoubleArraySorterTests {
    private void checkSort(Function<Random, Double> generator) {
        Random random = new Random();
        DoubleArraySorter sorter = new DoubleArraySorter(100);
        for (int times = 0; times < 1000; ++times) {
            int size = 1 + random.nextInt(100);
            int dim = 1 + random.nextInt(10);
            int obj = random.nextInt(dim);

            double[][] points = new double[size][dim];
            for (int i = 0; i < size; ++i) {
                for (int j = 0; j < dim; ++j) {
                    points[i][j] = generator.apply(random);
                }
            }
            int[] ranks = new int[size];
            Integer[] expectedRanks = new Integer[size];
            for (int i = 0; i < size; ++i) {
                ranks[i] = i;
                expectedRanks[i] = i;
            }
            Arrays.sort(expectedRanks, Comparator.comparingDouble(o -> points[o][obj]));
            sorter.sort(points, ranks, 0, size, obj);
            for (int i = 0; i < size; ++i) {
                Assert.assertEquals(points[expectedRanks[i]][obj], points[ranks[i]][obj], 1e-16);
            }
        }
    }

    @Test
    public void sortsRandomSequencesOK() {
        checkSort(Random::nextDouble);
    }

    @Test
    public void sortsBinarySequencesOK() {
        checkSort(random -> random.nextBoolean() ? 0.0 : 1.0);
    }

    @Test
    public void sortsSmallDomainSequencesOK() {
        checkSort(random -> (double) random.nextInt(10));
    }
}
