package ru.ifmo.util;

import java.util.concurrent.ThreadLocalRandom;

public class DoubleArraySorter {
    private final double[] scratch;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    private double[][] points = null;
    private int[] indices = null;
    private int coordinate = -1;
    private int maxCoordinate = -1;

    public DoubleArraySorter(int maximumPoints) {
        this.scratch = new double[maximumPoints];
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
}
