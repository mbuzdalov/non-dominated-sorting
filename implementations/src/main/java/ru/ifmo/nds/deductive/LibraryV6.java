package ru.ifmo.nds.deductive;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;

import java.util.Arrays;

public final class LibraryV6 extends NonDominatedSorting {
    public LibraryV6(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    public String getName() {
        return "Deductive Sort, library version 6";
    }

    @Override
    protected void closeImpl() {}

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = points.length;
        int dim = points[0].length;

        ArrayHelper.fillIdentity(indices, n);
        Arrays.fill(ranks, maximalMeaningfulRank + 1);

        for (int rank = 0, from = 0; from < n && rank <= maximalMeaningfulRank; ++rank) {
            from = iterate(indices, ranks, from, n, rank, points, dim);
        }
    }

    private static int iterate(int[] indices, int[] ranks, int curr, int last, int rank, double[][] points, int dim) {
        while (curr < last) {
            int currI = indices[curr];
            double[] currP = points[currI];
            int next = ++curr;
            int replayUntil = next;
            while (next < last) {
                int nextI = indices[next];
                int comparison = DominanceHelper.dominanceComparison(currP, points[nextI], dim);
                if (comparison == 0) {
                    ++next;
                } else {
                    indices[next] = indices[--last];
                    if (comparison < 0) {
                        indices[last] = nextI;
                    } else {
                        indices[last] = currI;
                        // Remember to scan the prefix since we updated the current point.
                        replayUntil = next;
                        currI = nextI;
                        currP = points[nextI];
                    }
                }
            }
            ranks[currI] = rank;
            if (replayUntil > curr) {
                // The current point got replaced at least once.
                // This means we need to scan the prefix [curr; replayUntil) once more.
                last = replay(indices, dim - 1, curr, last, replayUntil, currP, points);
            }
        }
        return last;
    }

    private static int replay(int[] indices, int maxObj, int lowLimit, int last, int index, double[] currP, double[][] points) {
        // Everything initially at index cannot be equal to currP and cannot dominate it.
        // This allows using an efficient comparison.
        // When looping from the end, we also simplify the logic vastly.
        while (--index >= lowLimit) {
            int nextI = indices[index];
            if (DominanceHelper.strictlyDominatesAssumingNotEqual(currP, points[nextI], maxObj)) {
                indices[index] = indices[--last];
                indices[last] = nextI;
            }
        }
        return last;
    }
}
