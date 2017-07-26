package ru.ifmo.util;

import java.util.concurrent.ThreadLocalRandom;

public class DoubleArraySorter {
    private final double[] scratch;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    private double[][] points = null;
    private int[] indices = null;
    private int coordinate = -1;
    private int maxCoordinate = -1;

    private double[] medianSwap;
    private int medianCount = 0, countLarger = -1, countSmaller = -1;
    private double lastMedianRunMin = Double.NaN, lastMedianRunMax = Double.NaN;

    public DoubleArraySorter(int maximumPoints) {
        this.scratch = new double[maximumPoints];
        this.medianSwap = new double[maximumPoints];
    }

    private void sortImplInside(int from, int until) {
        double pivot = scratch[random.nextInt(from, until)];
        int l = from, r = until - 1;
        while (l <= r) {
            while (scratch[l] < pivot) ++l;
            while (scratch[r] > pivot) --r;
            if (l <= r) {
                int tmpI = indices[l];
                indices[l] = indices[r];
                indices[r] = tmpI;
                double tmpD = scratch[l];
                scratch[l] = scratch[r];
                scratch[r] = tmpD;
                ++l;
                --r;
            }
        }
        if (from + 1 <= r) sortImplInside(from, r + 1);
        if (l + 1 < until) sortImplInside(l, until);
    }

    private void sortImpl(int from, int until) {
        for (int i = from; i < until; ++i) {
            scratch[i] = points[indices[i]][coordinate];
        }
        sortImplInside(from, until);
    }

    private void lexSortImpl(int from, int until, int coordinate) {
        this.coordinate = coordinate;
        sortImpl(from, until);

        int last = from;
        double lastX = points[indices[from]][coordinate];
        for (int i = from + 1; i < until; ++i) {
            double currX = points[indices[i]][coordinate];
            if (currX != lastX) {
                if (last + 1 < i && coordinate + 1 < maxCoordinate) {
                    lexSortImpl(last, i, coordinate + 1);
                }
                last = i;
                lastX = currX;
            }
        }
        if (last + 1 < until && coordinate + 1 < maxCoordinate) {
            lexSortImpl(last, until, coordinate + 1);
        }
    }

    public void sort(double[][] points, int[] indices, int from, int until, int whichCoordinate) {
        if (until - from > scratch.length) {
            throw new IllegalArgumentException("The maximum array length to be sorted is " + scratch.length
                    + ", but you requested from = " + from + " until = " + until + " which is " + (until - from));
        }
        this.points = points;
        this.indices = indices;
        this.coordinate = whichCoordinate;

        sortImpl(from, until);

        this.points = null;
        this.indices = null;
        this.coordinate = -1;
    }

    public void lexicographicalSort(double[][] points, int[] indices, int from, int until, int maxCoordinate) {
        if (until - from > scratch.length) {
            throw new IllegalArgumentException("The maximum array length to be sorted is " + scratch.length
                    + ", but you requested from = " + from + " until = " + until + " which is " + (until - from));
        }
        this.points = points;
        this.indices = indices;
        this.maxCoordinate = maxCoordinate;

        lexSortImpl(from, until, 0);

        this.points = null;
        this.indices = null;
        this.maxCoordinate = -1;
    }

    public void consumeDataForMedian(double[][] points, int[] indices, int from, int until, int whichCoordinate) {
        lastMedianRunMax = Double.NEGATIVE_INFINITY;
        lastMedianRunMin = Double.POSITIVE_INFINITY;
        for (int i = from; i < until; ++i) {
            double v = points[indices[i]][whichCoordinate];
            lastMedianRunMin = Math.min(lastMedianRunMin, v);
            lastMedianRunMax = Math.max(lastMedianRunMax, v);
            medianSwap[medianCount++] = v;
        }
    }

    public double getLastMedianConsumptionMax() {
        return lastMedianRunMax;
    }

    public double getLastMedianConsumptionMin() {
        return lastMedianRunMin;
    }

    public void resetMedian() {
        medianCount = 0;
    }

    public double findMedian() {
        medianImpl(0, medianCount, medianCount / 2);
        double rv = medianSwap[medianCount / 2];
        countLarger = countSmaller = 0;
        for (int i = 0; i < medianCount; ++i) {
            if (medianSwap[i] < rv) {
                ++countSmaller;
            } else if (medianSwap[i] > rv) {
                ++countLarger;
            }
        }
        return rv;
    }

    private void medianImpl(int from, int until, int index) {
        double pivot = medianSwap[random.nextInt(from, until)];
        int l = from, r = until - 1;
        while (l <= r) {
            while (medianSwap[l] < pivot) ++l;
            while (medianSwap[r] > pivot) --r;
            if (l <= r) {
                double tmp = medianSwap[l];
                medianSwap[l] = medianSwap[r];
                medianSwap[r] = tmp;
                ++l;
                --r;
            }
        }
        if (from <= index && index <= r) medianImpl(from, r + 1, index);
        if (l <= index && index < until) medianImpl(l, until, index);
    }

    public int howManyLargerThanMedian() {
        return countLarger;
    }

    public int howManySmallerThanMedian() {
        return countSmaller;
    }
}
