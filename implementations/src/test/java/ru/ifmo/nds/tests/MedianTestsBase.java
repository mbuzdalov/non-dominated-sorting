package ru.ifmo.nds.tests;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;

import ru.ifmo.nds.util.ArrayHelper;

public abstract class MedianTestsBase {
    protected abstract double destructiveMedian(double[] array, int until);

    private void checkMedian(Function<Random, Double> generator) {
        Random random = new Random();
        double[] medianSwap = new double[100];

        for (int times = 0; times < 100; ++times) {
            for (int size = 1; size <= 100; ++size) {
                double[] points = new double[size];
                int[] indices = new int[size];
                for (int i = 0; i < size; ++i) {
                    points[i] = generator.apply(random);
                    indices[i] = i;
                }
                for (int i = 0; i < size; ++i) {
                    ArrayHelper.swap(indices, i, random.nextInt(i + 1));
                }

                ArrayHelper.transplant(points, indices, 0, size, medianSwap, 0);
                double median = destructiveMedian(medianSwap, size);
                double[] originalPoints = points.clone();
                Arrays.sort(points);
                Assert.assertEquals(Arrays.toString(originalPoints), points[size / 2], median, 1e-16);
            }
        }
    }

    @Test
    public void findsMedianInRandomSequencesOK() {
        checkMedian(Random::nextDouble);
    }

    @Test
    public void findsMedianInBinarySequencesOK() {
        checkMedian(random -> random.nextBoolean() ? 0.0 : 1.0);
    }

    @Test
    public void findsMedianInSmallDomainSequencesOK() {
        checkMedian(random -> (double) random.nextInt(10));
    }
}
