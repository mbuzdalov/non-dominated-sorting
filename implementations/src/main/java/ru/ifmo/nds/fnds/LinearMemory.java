package ru.ifmo.nds.fnds;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;
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

    private void doSorting(int n, int dim, int maximalMeaningfulRank) {
        ranks[0] = 0;
        int maxObj = dim - 1;
        for (int i = 1; i < n; ++i) {
            int myRank = 0;
            double[] pi = points[i];
            for (int j = i - 1; j >= 0; --j) {
                int thatRank = ranks[j];
                if (myRank <= thatRank && DominanceHelper.strictlyDominatesAssumingNotSame(points[j], pi, maxObj)) {
                    myRank = thatRank + 1;
                    if (myRank > maximalMeaningfulRank) {
                        break;
                    }
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
