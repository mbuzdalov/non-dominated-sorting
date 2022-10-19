package ru.ifmo.nds.ndt;

import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.SplitMergeHelper;
import ru.ifmo.nds.util.median.DestructiveMedianAlgorithm;
import ru.ifmo.nds.util.median.HoareBidirectionalScan;

public class SplitBuilder {
    private final double[] medianSwap;
    private final int[] indices;
    private final double[][] transposedPoints;
    private final int threshold;
    private final SplitMergeHelper splitMerge;
    private final Split[] splits;
    private final DestructiveMedianAlgorithm destructiveMedian;

    public SplitBuilder(double[][] transposedPoints, int size, int threshold) {
        this.transposedPoints = transposedPoints;
        this.medianSwap = new double[size];
        this.indices = new int[size];
        this.splitMerge = new SplitMergeHelper(size);
        this.splits = new Split[size];
        for (int i = 0; i < size; ++i) {
            splits[i] = new Split();
        }
        this.threshold = threshold;
        this.destructiveMedian = HoareBidirectionalScan.instance().createInstance(size);
    }

    private Split construct(int from, int until, int coordinate, int depth, int[] maxCoordinateNSplits) {
        if (from + threshold < until) {
            int nextCoordinate = coordinate + 1 == maxCoordinateNSplits[0] ? 1 : coordinate + 1;
            double min = ArrayHelper.transplantAndReturnMinIfNotSameElseNaN(transposedPoints[coordinate], indices, from, until, medianSwap, from);
            if (Double.isNaN(min)) {
                if (depth == maxCoordinateNSplits[0]) {
                    // When all median values are equal for all remaining coordinates,
                    // we have no choice other to fail splitting
                    return Split.NULL_MAX_DEPTH;
                } else {
                    return construct(from, until, nextCoordinate, depth + 1, maxCoordinateNSplits);
                }
            }
            double median = destructiveMedian.solve(medianSwap, from, until);
            if (min == median) {
                // It can be that median equals to everything from [0; n/2].
                // This will make a "0 vs n" split and the subsequent stack overflow.
                // To prevent this, we will increase the median slightly.
                median = Math.nextUp(median);
            }
            int mid = splitMerge.splitInTwo(transposedPoints[coordinate], indices,
                    from, from, until, median, false);
            Split rv = splits[maxCoordinateNSplits[1]];
            ++maxCoordinateNSplits[1];
            rv.initialize(coordinate, median,
                    construct(from, mid, nextCoordinate, 0, maxCoordinateNSplits),
                    construct(mid, until, nextCoordinate, 0, maxCoordinateNSplits));
            return rv;
        } else {
            return Split.NULL_POINTS;
        }
    }

    public Split result(int nPoints, int dimension) {
        ArrayHelper.fillIdentity(indices, nPoints);
        return construct(0, nPoints, 1, 0, new int[] { dimension, 0 });
    }

    public Split result(int from, int until, int[] indices, int dimension) {
        System.arraycopy(indices, from, this.indices, from, until - from);
        return construct(from, until, 1, 0, new int[] { dimension, from });
    }
}
