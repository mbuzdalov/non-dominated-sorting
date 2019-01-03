package ru.ifmo.nds.tests;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Random;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;
import ru.ifmo.nds.util.ArraySorter;

public class ArraySorterTests {
    private void checkSort(Function<Random, Double> generator) {
        Random random = new Random();
        ArraySorter sorter = new ArraySorter(100);
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
    public void checkSortComparingByIndicesIfEqual() {
        Random random = new Random();
        ArraySorter sorter = new ArraySorter(100);
        for (int times = 0; times < 1000; ++times) {
            int size = 1 + random.nextInt(100);
            boolean isDiscrete = random.nextBoolean();
            double[][] array = new double[size][1];
            int[] indices = new int[size];
            Integer[] expectedIndices = new Integer[size];
            for (int i = 0; i < size; ++i) {
                indices[i] = i;
                expectedIndices[i] = i;
                array[i][0] = isDiscrete ? random.nextInt(5) : random.nextDouble();
            }
            Arrays.sort(expectedIndices, Comparator.comparingDouble((Integer o) -> array[o][0]));
            sorter.sortComparingByIndicesIfEqual(array, indices, 0, size, 0);
            for (int i = 0; i < size; ++i) {
                Assert.assertEquals(expectedIndices[i].intValue(), indices[i]);
            }
        }
    }
}
