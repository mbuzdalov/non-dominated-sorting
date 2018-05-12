package ru.ifmo.nds.ens;

import java.util.Arrays;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DoubleArraySorter;

public abstract class ENSBase extends NonDominatedSorting {
    private int[] prevIndex;
    private int[] lastRankIndex;

    ENSBase(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        prevIndex = new int[maximumPoints];
        lastRankIndex = new int[maximumPoints];
    }

    @Override
    protected void closeImpl() {
        prevIndex = null;
        lastRankIndex = null;
    }

    boolean frontDominates(int frontIndex, double[][] points, double[] point) {
        return frontDominatesWithWork(frontIndex, points, point) >= 0;
    }

    int findRankByBinarySearch(double[][] points, double[] point, int minRank, int maxRank) {
        int leftRank = minRank, rightRank = maxRank + 1;
        while (rightRank - leftRank > 1) {
            int currRank = (leftRank + rightRank) >>> 1;
            if (frontDominates(currRank, points, point)) {
                leftRank = currRank;
            } else {
                rightRank = currRank;
            }
        }
        return rightRank;
    }

    private boolean strictlyDominatesAssumingNotSame(double[] good, double[] weak, int dim) {
        for (int i = dim - 1; i > 0; --i) {
            if (good[i] > weak[i]) {
                return false;
            }
        }
        return true;
    }

    int frontDominatesWithWork(int frontIndex, double[][] points, double[] point) {
        int index = lastRankIndex[frontIndex];
        int dim = point.length;
        if (dim == 2) {
            // This is essentially how the 2D case of JFB works.
            return strictlyDominatesAssumingNotSame(points[index], point, dim) ? 1 : -1;
        } else {
            int count = 0;
            while (index >= 0) {
                ++count;
                if (strictlyDominatesAssumingNotSame(points[index], point, dim)) {
                    return count;
                }
                index = prevIndex[index];
            }
            return -count;
        }
    }

    int setRank(int pointIndex, int[] ranks, int rank, int maxRank, int maximumMeaningfulRank) {
        if (rank > maximumMeaningfulRank) {
            ranks[pointIndex] = maximumMeaningfulRank + 1;
        } else {
            if (rank > maxRank) {
                maxRank = rank;
                lastRankIndex[maxRank] = -1;
            }
            prevIndex[pointIndex] = lastRankIndex[rank];
            lastRankIndex[rank] = pointIndex;
            ranks[pointIndex] = rank;
        }
        return maxRank;
    }

    abstract void sortCheckedImpl(double[][] points, int[] ranks, int maximalMeaningfulRank);

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = ranks.length;
        ArrayHelper.fillIdentity(indices, n);
        Arrays.fill(prevIndex, -1);
        sorter.lexicographicalSort(points, indices, 0, n, points[0].length);
        sortCheckedImpl(points, ranks, maximalMeaningfulRank);
    }
}
