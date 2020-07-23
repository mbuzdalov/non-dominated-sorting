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
        final int[] indices = this.indices;
        final int n = points.length;
        final int dim = points[0].length;

        ArrayHelper.fillIdentity(indices, n);
        Arrays.fill(ranks, maximalMeaningfulRank + 1);

        int from = 0;
        for (int rank = 0; from < n && rank <= maximalMeaningfulRank; ++rank) {
            int curr = from;
            int last = n;
            while (curr < last) {
                int currI = indices[curr];
                double[] currP = points[currI];
                int next = curr + 1;
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
                            replayUntil = next;
                            currI = nextI;
                            currP = points[nextI];
                        }
                    }
                }
                ranks[currI] = rank;
                ++curr;
                if (replayUntil > curr) {
                    // The current point got replaced at least once.
                    // This means we need to scan the prefix [curr; replayUntil) once more.
                    last = replay(indices, dim, curr, last, replayUntil, currP, points);
                }
            }
            from = last;
        }
    }

    private static int replay(int[] indices, int dim, int curr, int last, int replayUntil, double[] currP, double[][] points) {
        int maxObj = dim - 1;
        int next = curr;
        // Initial mode: next < replayUntil <= last
        // While this holds, everything initially at next cannot be equal to currP.
        // Additionally, before replayUntil, there are no points equal to currP, hence we can use an efficient comparison
        while (next < replayUntil) {
            int nextI = indices[next];
            if (DominanceHelper.strictlyDominatesAssumingNotEqual(currP, points[nextI], maxObj)) {
                indices[next] = indices[--last];
                indices[last] = nextI;
                if (last < replayUntil) {
                    // Before this happened, we moved forward the points that were already checked for dominance.
                    // This allows the unconditional ++next and saves comparisons.
                    // Now we start moving non-tested points forward, need to switch the technique.
                    return replayConservative(indices, dim, next, last, currP, points);
                }
            }
            ++next;
        }
        return last;
    }

    private static int replayConservative(int[] indices, int dim, int next, int last, double[] currP, double[][] points) {
        while (next < last) {
            int nextI = indices[next];
            if (DominanceHelper.strictlyDominates(currP, points[nextI], dim)) {
                indices[next] = indices[--last];
                indices[last] = nextI;
            } else {
                ++next;
            }
        }
        return last;
    }
}
