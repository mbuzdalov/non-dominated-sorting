package ru.ifmo.nds.ndt;

import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.SplitMergeHelper;

public class SplitBuilder {
    private final double[] medianSwap;
    private final int[] indices;
    private final double[][] transposedPoints;
    private int threshold;
    private int maxCoordinate;
    private final SplitMergeHelper splitMerge;
    private final Split[] splits;
    private int nSplits;

    public SplitBuilder(double[][] transposedPoints, int size) {
        this.transposedPoints = transposedPoints;
        this.medianSwap = new double[size];
        this.indices = new int[size];
        this.splitMerge = new SplitMergeHelper(size);
        this.splits = new Split[size];
        for (int i = 0; i < size; ++i) {
            splits[i] = new Split();
        }
    }

    private Split construct(int from, int until, int coordinate, int depth) {
        if (from + threshold < until) {
            int nextCoordinate = coordinate + 1 == maxCoordinate ? 1 : coordinate + 1;
            double min = ArrayHelper.transplantAndReturnMinIfNotSameElseNaN(transposedPoints[coordinate], indices, from, until, medianSwap, from);
            if (Double.isNaN(min)) {
                if (depth == maxCoordinate) {
                    // When all median values are equal for all remaining coordinates,
                    // we have no choice other to fail splitting
                    return Split.NULL_MAX_DEPTH;
                } else {
                    return construct(from, until, nextCoordinate, depth + 1);
                }
            }
            double median = ArrayHelper.destructiveMedian(medianSwap, from, until);
            if (min == median) {
                // It can be that median equals to everything from [0; n/2].
                // This will make a "0 vs n" split and the subsequent stack overflow.
                // To prevent this, we will increase the median slightly.
                median = Math.nextUp(median);
            }
            int mid = splitMerge.splitInTwo(transposedPoints[coordinate], indices,
                    from, from, until, median, false);
            Split rv = splits[nSplits++];
            rv.initialize(coordinate, median,
                    construct(from, mid, nextCoordinate, 0),
                    construct(mid, until, nextCoordinate, 0));
            return rv;
        } else {
            return Split.NULL_POINTS;
        }
    }

    public Split result(int nPoints, int dimension, int threshold) {
        this.threshold = threshold;
        this.maxCoordinate = dimension;
        this.nSplits = 0;
        ArrayHelper.fillIdentity(indices, nPoints);
        Split result = construct(0, nPoints, 1, 0);
        this.threshold = -1;
        return result;
    }

    public Split result(int from, int until, int[] indices, int dimension, int threshold) {
        this.threshold = threshold;
        this.maxCoordinate = dimension;
        this.nSplits = 0;
        System.arraycopy(indices, from, this.indices, from, until - from);
        Split result = construct(from, until, 1, 0);
        this.threshold = -1;
        return result;
    }
}
