package ru.ifmo.ens;

import ru.ifmo.NonDominatedSorting;
import ru.ifmo.util.DoubleArraySorter;

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
    protected void closeImpl() throws Exception {
        sorter = null;
        indices = null;
        prevIndex = null;
        lastRankIndex = null;
    }

    private boolean dominates(double[] a, double[] b) {
        boolean hasLess = false;
        int d = a.length;
        for (int i = 0; i < d; ++i) {
            if (a[i] > b[i]) {
                return false;
            }
            hasLess |= a[i] < b[i];
        }
        return hasLess;
    }

    boolean frontDominates(int frontIndex, double[][] points, double[] point) {
        int index = lastRankIndex[frontIndex];
        while (index >= 0) {
            if (dominates(points[index], point)) {
                return true;
            }
            index = prevIndex[index];
        }
        return false;
    }

    int setRank(int pointIndex, int[] ranks, int rank, int maxRank) {
        if (rank > maxRank) {
            maxRank = rank;
            lastRankIndex[maxRank] = -1;
        }
        prevIndex[pointIndex] = lastRankIndex[rank];
        lastRankIndex[rank] = pointIndex;
        ranks[pointIndex] = rank;
        return maxRank;
    }

    abstract void sortCheckedImpl(double[][] points, int[] ranks, int maximalMeaningfulRank);

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = ranks.length;
        for (int i = 0; i < n; ++i) {
            indices[i] = i;
        }
        Arrays.fill(prevIndex, -1);
        sorter.lexicographicalSort(points, indices, 0, n, points[0].length);
        sortCheckedImpl(points, ranks, maximalMeaningfulRank);
    }
}
