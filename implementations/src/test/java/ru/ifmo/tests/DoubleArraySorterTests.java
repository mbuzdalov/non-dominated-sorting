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

    @Test
    public void checkSortWithResolver() {
        Random random = new Random();
        DoubleArraySorter sorter = new DoubleArraySorter(100);
        for (int times = 0; times < 1000; ++times) {
            int size = 1 + random.nextInt(100);
            double[][] array = new double[size][1];
            int[] resolver = new int[size];
            int[] indices = new int[size];
            Integer[] expectedIndices = new Integer[size];
            for (int i = 0; i < size; ++i) {
                indices[i] = i;
                expectedIndices[i] = i;
                array[i][0] = random.nextDouble();
                resolver[i] = random.nextInt();
            }
            Arrays.sort(expectedIndices, Comparator.comparingDouble((Integer o) -> array[o][0]).thenComparingInt(o -> resolver[o]));
            sorter.sortWhileResolvingEqual(array, indices, 0, size, 0, resolver);
            for (int i = 0; i < size; ++i) {
                Assert.assertEquals(array[expectedIndices[i]][0], array[indices[i]][0], 1e-16);
            }
        }
    }
}
