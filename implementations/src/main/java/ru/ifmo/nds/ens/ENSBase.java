package ru.ifmo.nds.ens;

import java.util.Arrays;

import static ru.ifmo.nds.util.DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller;

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
        int index = lastRankIndex[frontIndex];
        int maxObj = point.length - 1;
        if (maxObj == 1) {
            // This is essentially how the 2D case of JFB works.
            return strictlyDominatesAssumingLexicographicallySmaller(points[index], point, maxObj);
        } else {
            while (index >= 0) {
                if (strictlyDominatesAssumingLexicographicallySmaller(points[index], point, maxObj)) {
                    return true;
                }
                index = prevIndex[index];
            }
            return false;
        }
    }

    private int setRank(int pointIndex, int[] ranks, int rank, int maxRank, int maximumMeaningfulRank) {
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

    abstract int findRank(double[][] points, double[] curr, int maxRank);

    private void sortCheckedImpl(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        final int n = ranks.length;
        int i0 = indices[0];
        double[] prev = points[i0];
        setRank(i0, ranks, 0, -1, maximalMeaningfulRank);
        int prevRank = 0;
        int maxRank = 0;
        final int len = prev.length;
        for (int i = 1; i < n; ++i) {
            int index = indices[i];
            double[] curr = points[index];
            int currRank;
            if (ArrayHelper.equal(prev, curr, len)) {
                currRank = prevRank;
            } else {
                prevRank = currRank = findRank(points, curr, maxRank);
                prev = curr;
            }
            maxRank = setRank(index, ranks, currRank, maxRank, maximalMeaningfulRank);
        }
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = ranks.length;
        ArrayHelper.fillIdentity(indices, n);
        Arrays.fill(prevIndex, 0, n, -1);
        sorter.lexicographicalSort(points, indices, 0, n, points[0].length);
        sortCheckedImpl(points, ranks, maximalMeaningfulRank);
    }
}
