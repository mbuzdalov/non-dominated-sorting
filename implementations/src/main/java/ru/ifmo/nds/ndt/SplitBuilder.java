package ru.ifmo.nds.ndt;

import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.SplitMergeHelper;

public class SplitBuilder {
    private final double[] medianSwap;
    private final int[] indices;
    private double[][] transposedPoints;
    private int threshold;
    private int maxCoordinate;
    private final SplitMergeHelper splitMerge;
    private final Split[] splits;
    private int nSplits;

    public SplitBuilder(int size) {
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
            ArrayHelper.transplant(transposedPoints[coordinate], indices, from, until, medianSwap, from);
            double median = ArrayHelper.destructiveMedian(medianSwap, from, until);
            double min = ArrayHelper.min(medianSwap, from, until);
            double max = ArrayHelper.max(medianSwap, from, until);
            if (min == max) {
                if (depth == maxCoordinate) {
                    // When all median values are equal for all remaining coordinates,
                    // we have no choice other to fail splitting
                    return Split.NullMaxDepth.INSTANCE;
                } else {
                    return construct(from, until, nextCoordinate, depth + 1);
                }
            }
            if (min == median) {
                // It can be that median equals to everything from [0; n/2].
                // This will make a "0 vs n" split and the subsequent stack overflow.
                // To prevent this, we will increase the median slightly.
                median = Math.nextUp(median);
            }
            int mid = splitMerge.splitInTwo(transposedPoints[coordinate], indices,
                    from, from, until, median, false, min, max);
            Split rv = splits[nSplits++];
            rv.initialize(coordinate, median,
                    construct(from, mid, nextCoordinate, 0),
                    construct(mid, until, nextCoordinate, 0));
            return rv;
        } else {
            return Split.NullPoints.INSTANCE;
        }
    }

    public Split result(double[][] transposedPoints, int nPoints, int dimension, int threshold) {
        return result(transposedPoints, 0, nPoints, dimension, threshold);
    }

    public Split result(double[][] transposedPoints, int from, int until, int dimension, int threshold) {
        this.transposedPoints = transposedPoints;
        this.threshold = threshold;
        this.maxCoordinate = dimension;
        this.nSplits = 0;
        for (int i = from; i < until; ++i) {
            indices[i] = i;
        }
        Split result = construct(from, until, 1, 0);
        this.transposedPoints = null;
        this.threshold = -1;
        return result;
    }
}
