package ru.ifmo.nds.ens;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;
import ru.ifmo.nds.util.DoubleArraySorter;

import java.util.Arrays;

public abstract class ENSBase extends NonDominatedSorting {
    int[] indices;
    private int[] prevIndex;
    private int[] lastRankIndex;
    private DoubleArraySorter sorter;

    ENSBase(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        sorter = new DoubleArraySorter(maximumPoints);
        indices = new int[maximumPoints];
        prevIndex = new int[maximumPoints];
        lastRankIndex = new int[maximumPoints];
    }

    @Override
    protected void closeImpl() {
        sorter = null;
        indices = null;
        prevIndex = null;
        lastRankIndex = null;
    }

    boolean frontDominates(int frontIndex, double[][] points, double[] point) {
        int index = lastRankIndex[frontIndex];
        int dim = point.length;
        while (index >= 0) {
            if (DominanceHelper.strictlyDominates(points[index], point, dim)) {
                return true;
            }
            index = prevIndex[index];
        }
        return false;
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
