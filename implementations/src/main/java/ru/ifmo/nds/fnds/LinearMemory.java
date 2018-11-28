package ru.ifmo.nds.fnds;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;

public class LinearMemory extends NonDominatedSorting {
    private double[][] testedPoints;
    private int[] testedPointRanks;

    public LinearMemory(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        testedPoints = new double[maximumPoints][];
        testedPointRanks = new int[maximumPoints];
    }

    @Override
    public String getName() {
        return "Fast Non-Dominated Sorting (with linear memory)";
    }

    @Override
    protected void closeImpl() {
        testedPoints = null;
        testedPointRanks = null;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        // 1. Getting the points sorted.
        final int n = points.length;
        final int dim = points[0].length;
        final int maxObj = dim - 1;
        ArrayHelper.fillIdentity(indices, n);
        sorter.lexicographicalSort(points, indices, 0, n, dim);

        // 2. The first point always has a rank 0
        int i0 = indices[0];
        ranks[i0] = 0;
        double[] lastPoint = points[i0];
        int lastRank = 0;
        testedPoints[0] = lastPoint;
        testedPointRanks[0] = 0; // only to enhance readability, holds by default
        int nTestedPoints = 1;

        // 3. Scanning the rest of the points against the previously tested points.
        for (int i = 1; i < n; ++i) {
            int index = indices[i];
            double[] currPoint = points[index];
            if (ArrayHelper.equal(lastPoint, currPoint, dim)) {
                ranks[index] = lastRank;
            } else {
                int currRank = 0;
                // The entire machinery with the testing points is only to ensure more-or-less sequential memory access.
                for (int j = nTestedPoints - 1; j >= 0; --j) {
                    int jRank = testedPointRanks[j];
                    if (currRank <= jRank && DominanceHelper.strictlyDominatesAssumingNotSame(testedPoints[j], currPoint, maxObj)) {
                        currRank = jRank + 1;
                        if (currRank > maximalMeaningfulRank) {
                            break;
                        }
                    }
                }
                ranks[index] = currRank;
                if (currRank <= maximalMeaningfulRank) {
                    // Maybe run insertion sort to ensure the ranks are sorted
                    // (and that it is valid to fall off, in the loop above, once dominated)?
                    testedPointRanks[nTestedPoints] = currRank;
                    testedPoints[nTestedPoints] = currPoint;
                    ++nTestedPoints;
                }
                lastPoint = currPoint;
                lastRank = currRank;
            }
        }
    }
}
