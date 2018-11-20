package ru.ifmo.nds.ens;

import java.util.Arrays;

import static ru.ifmo.nds.util.DominanceHelper.strictlyDominatesAssumingNotSame;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;

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

    final boolean frontDominates(int frontIndex, double[][] points, double[] point) {
        return frontDominatesWithWork(frontIndex, points, point) >= 0;
    }

    final int findRankByBinarySearch(double[][] points, double[] point, int minRank, int maxRank) {
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

    final int frontDominatesWithWork(int frontIndex, double[][] points, double[] point) {
        int index = lastRankIndex[frontIndex];
        int maxObj = point.length - 1;
        if (maxObj == 1) {
            // This is essentially how the 2D case of JFB works.
            return strictlyDominatesAssumingNotSame(points[index], point, maxObj) ? 1 : -1;
        } else {
            int count = 0;
            while (index >= 0) {
                ++count;
                if (strictlyDominatesAssumingNotSame(points[index], point, maxObj)) {
                    return count;
                }
                index = prevIndex[index];
            }
            return -count;
        }
    }

    final int setRank(int pointIndex, int[] ranks, int rank, int maxRank, int maximumMeaningfulRank) {
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
        Arrays.fill(prevIndex, 0, n, -1);
        sorter.lexicographicalSort(points, indices, 0, n, points[0].length);
        sortCheckedImpl(points, ranks, maximalMeaningfulRank);
    }
}
