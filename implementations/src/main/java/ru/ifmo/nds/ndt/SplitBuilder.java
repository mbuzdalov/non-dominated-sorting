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

    public SplitBuilder(int size) {
        this.medianSwap = new double[size];
        this.indices = new int[size];
        this.splitMerge = new SplitMergeHelper(size);
    }

    private Split construct(int from, int until, int coordinate, int depth) {
        if (from + threshold < until) {
            int nextCoordinate = coordinate + 1 == maxCoordinate ? 1 : coordinate + 1;
            ArrayHelper.transplant(transposedPoints[coordinate], indices, from, until, medianSwap, from);
            double median = ArrayHelper.destructiveMedian(medianSwap, from, until);
            int countSmaller = ArrayHelper.countSmaller(medianSwap, from, until, median);
            if (countSmaller == 0) {
                // It can be that median equals to everything from [0; n/2].
                // This will make a "0 vs n" split and the subsequent stack overflow.
                // To prevent this, we will increase the median slightly.
                median = Math.nextUp(median);
            }
            double min = ArrayHelper.min(medianSwap, from, until);
            double max = ArrayHelper.max(medianSwap, from, until);
            int mid = splitMerge.splitInTwo(transposedPoints[coordinate], indices,
                    from, from, until, median, false, min, max);
            return new Split(coordinate, median,
                    construct(from, mid, nextCoordinate, depth + 1),
                    construct(mid, until, nextCoordinate, depth + 1));
        } else {
            return null;
        }
    }

    public Split result(double[][] transposedPoints, int nPoints, int dimension, int threshold) {
        this.transposedPoints = transposedPoints;
        this.threshold = threshold;
        this.maxCoordinate = dimension;
        for (int i = transposedPoints[0].length - 1; i >= 0; --i) {
            indices[i] = i;
        }
        Split result = construct(0, nPoints, 1, 0);
        this.transposedPoints = null;
        this.threshold = -1;
        return result;
    }
}
