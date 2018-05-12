package ru.ifmo.nds.fnds;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DoubleArraySorter;

public class LinearMemory extends NonDominatedSorting {
    private int[] ranks;
    private double[][] points;

    public LinearMemory(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        ranks = new int[maximumPoints];
        points = new double[maximumPoints][];
    }

    @Override
    public String getName() {
        return "Fast Non-Dominated Sorting (with linear memory)";
    }

    @Override
    protected void closeImpl() {
        ranks = null;
        points = null;
    }

    private boolean strictlyDominatesAssumingNotSame(int goodIndex, int weakIndex, int dim) {
        double[] goodPoint = points[goodIndex];
        double[] weakPoint = points[weakIndex];
        // Comparison in 0 makes no sense, as due to goodIndex < weakIndex the points are <= in this coordinate.
        for (int i = dim - 1; i > 0; --i) {
            if (goodPoint[i] > weakPoint[i]) {
                return false;
            }
        }
        return true;
    }

    private void doSorting(int n, int dim, int maximalMeaningfulRank) {
        ranks[0] = 0;
        for (int i = 1; i < n; ++i) {
            int myRank = 0;
            for (int j = i - 1; myRank <= maximalMeaningfulRank && j >= 0; --j) {
                int thatRank = ranks[j];
                if (myRank <= thatRank && strictlyDominatesAssumingNotSame(j, i, dim)) {
                    myRank = thatRank + 1;
                }
            }
            ranks[i] = myRank;
        }
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = points.length;
        int dim = points[0].length;
        ArrayHelper.fillIdentity(indices, n);
        sorter.lexicographicalSort(points, indices, 0, n, dim);
        int newN = DoubleArraySorter.retainUniquePoints(points, indices, this.points, ranks);
        doSorting(newN, dim, maximalMeaningfulRank);
        for (int i = 0; i < n; ++i) {
            ranks[i] = this.ranks[ranks[i]];
            this.points[i] = null;
        }
    }
}
