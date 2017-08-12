package ru.ifmo.nds.tests;

import java.util.Arrays;
import java.util.Random;
import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;
import ru.ifmo.nds.util.MedianFinder;

public class MedianFinderTests {
    private void checkMedian(Function<Random, Double> generator) {
        Random random = new Random();
        MedianFinder sorter = new MedianFinder(100);
        for (int times = 0; times < 1000; ++times) {
            int size = 1 + random.nextInt(100);

            double[] points = new double[size];
            int[] indices = new int[size];
            for (int i = 0; i < size; ++i) {
                points[i] = generator.apply(random);
                indices[i] = i;
            }
            sorter.resetMedian();
            sorter.consumeDataForMedian(points, indices, 0, size);
            double median = sorter.findMedian();
            Arrays.sort(points);
            Assert.assertEquals(points[size / 2], median, 1e-16);
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
