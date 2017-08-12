package ru.ifmo.nds.util;

import java.util.concurrent.ThreadLocalRandom;

public class DoubleArraySorter {
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

    private void sortByResolver(int from, int until) {
        int pivot = resolver[indices[random.nextInt(from, until)]];
        int l = from, r = until - 1;
        while (l <= r) {
            while (resolver[indices[l]] < pivot) ++l;
            while (resolver[indices[r]] > pivot) --r;
            if (l <= r) {
                int tmpI = indices[l];
                indices[l] = indices[r];
                indices[r] = tmpI;
                ++l;
                --r;
            }
        }
        if (from + 1 <= r) sortByResolver(from, r + 1);
        if (l + 1 < until) sortByResolver(l, until);
    }

    private void sortWhileResolvingEqualImpl(int from, int until) {
        sortImpl(from, until);

        int last = from;
        double lastX = points[indices[from]][coordinate];
        for (int i = from + 1; i < until; ++i) {
            double currX = points[indices[i]][coordinate];
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
        int n = sourcePoints.length, dim = sourcePoints[0].length, newN = 1;
        int lastII = sortedIndices[0];
        targetPoints[0] = sourcePoints[lastII];
        reindex[lastII] = 0;
        for (int i = 1; i < n; ++i) {
            int currII = sortedIndices[i];
            double[] curr = sourcePoints[lastII];
            double[] next = sourcePoints[currII];
            boolean same = true;
            for (int t = dim - 1; t >= 0; --t) {
                if (curr[t] != next[t]) {
                    same = false;
                    break;
                }
            }
            if (!same) {
                // Copying the point to the internal array.
                targetPoints[newN] = sourcePoints[currII];
                lastII = currII;
                ++newN;
            }

            // Abusing the argument "ranks" array to keep where the reference point lies.
            reindex[currII] = newN - 1;
        }
        return newN;
    }
}
