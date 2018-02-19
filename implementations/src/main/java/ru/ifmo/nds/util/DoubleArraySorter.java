package ru.ifmo.nds.util;

import java.util.concurrent.ThreadLocalRandom;

public final class DoubleArraySorter {
    private final double[] scratch;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    private double[][] points = null;
    private int[] indices = null;
    private int coordinate = -1;
    private int maxCoordinate = -1;
    private int[] resolver = null;

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
                ArrayHelper.swap(indices, l, r);
                ArrayHelper.swap(scratch, l++, r--);
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

        if (coordinate + 1 < maxCoordinate) {
            int last = from;
            double lastX = scratch[from];
            for (int i = from + 1; i < until; ++i) {
                double currX = scratch[i];
                if (currX != lastX) {
                    if (last + 1 < i) {
                        lexSortImpl(last, i, coordinate + 1);
                    }
                    last = i;
                    lastX = currX;
                }
            }
            if (last + 1 < until) {
                lexSortImpl(last, until, coordinate + 1);
            }
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

    private void sortByResolver(int from, int until) {
        int pivot = resolver[indices[random.nextInt(from, until)]];
        int l = from, r = until - 1;
        while (l <= r) {
            while (resolver[indices[l]] < pivot) ++l;
            while (resolver[indices[r]] > pivot) --r;
            if (l <= r) {
                ArrayHelper.swap(indices, l++, r--);
            }
        }
        if (from + 1 <= r) sortByResolver(from, r + 1);
        if (l + 1 < until) sortByResolver(l, until);
    }

    private void sortWhileResolvingEqualImpl(int from, int until) {
        sortImpl(from, until);

        // after sortImpl, scratch[i] == points[indices[i]][coordinate]
        // sortByResolver does not alter scratch.

        int last = from;
        double lastX = scratch[from];
        for (int i = from + 1; i < until; ++i) {
            double currX = scratch[i];
            if (currX != lastX) {
                if (last + 1 < i) {
                    sortByResolver(last, i);
                }
                last = i;
                lastX = currX;
            }
        }
        if (last + 1 < until) {
            sortByResolver(last, until);
        }
    }

    public void sortWhileResolvingEqual(double[][] points, int[] indices, int from, int until, int coordinate, int[] resolver) {
        if (until - from > scratch.length) {
            throw new IllegalArgumentException("The maximum array length to be sorted is " + scratch.length
                    + ", but you requested from = " + from + " until = " + until + " which is " + (until - from));
        }
        this.points = points;
        this.indices = indices;
        this.coordinate = coordinate;
        this.resolver = resolver;

        sortWhileResolvingEqualImpl(from, until);

        this.points = null;
        this.indices = null;
        this.coordinate = -1;
        this.resolver = null;
    }

    public static int retainUniquePoints(double[][] sourcePoints, int[] sortedIndices, double[][] targetPoints, int[] reindex) {
        int n = sourcePoints.length;
        return retainUniquePoints(sourcePoints, sortedIndices, targetPoints, reindex, n);
    }

    public static int retainUniquePoints(double[][] sourcePoints,
                                         int[] sortedIndices,
                                         double[][] targetPoints,
                                         int[] reindex,
                                         int N) {
        return retainUniquePoints(sourcePoints, sortedIndices, targetPoints, reindex, N, sourcePoints[0].length);
    }

    public static int retainUniquePoints(double[][] sourcePoints, int[] sortedIndices, double[][] targetPoints, int[] reindex, int N, int M) {
        int newN = 1;
        int lastII = sortedIndices[0];
        targetPoints[0] = sourcePoints[lastII];
        reindex[lastII] = 0;
        for (int i = 1; i < N; ++i) {
            int currII = sortedIndices[i];
            if (!ArrayHelper.equal(sourcePoints[lastII], sourcePoints[currII], M)) {
                // Copying the point to the internal array.
                targetPoints[newN] = sourcePoints[currII];
                lastII = currII;
                ++newN;
            }
            reindex[currII] = newN - 1;
        }
        return newN;
    }
}
