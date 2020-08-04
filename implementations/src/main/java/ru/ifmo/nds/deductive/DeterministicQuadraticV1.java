package ru.ifmo.nds.deductive;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;

import java.util.Arrays;

public final class DeterministicQuadraticV1 extends NonDominatedSorting {
    public DeterministicQuadraticV1(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    public String getName() {
        return "Deductive Sort, deterministic worst-case quadratic version, naive implementation";
    }

    @Override
    protected void closeImpl() {}

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        final int[] indices = this.indices;
        final int n = points.length;
        final int dim = points[0].length;
        final int maxObj = dim - 1;

        ArrayHelper.fillIdentity(indices, n);
        Arrays.fill(ranks, maximalMeaningfulRank + 1);

        int from = 0;
        for (int rank = 0; from < n && rank <= maximalMeaningfulRank; ++rank) {
            int curr = from;
            int last = n;
            while (curr < last) {
                int currI = indices[curr];
                double[] currP = points[currI];
                // First stage. Find the best dominating point for curr
                for (int next = curr + 1; next < last; ++next) {
                    int nextI = indices[next];
                    double[] nextP = points[nextI];
                    if (DominanceHelper.strictlyDominatesAssumingNotEqual(nextP, currP, maxObj)) {
                        indices[next] = currI;
                        currI = nextI;
                        currP = nextP;
                    }
                }
                ranks[currI] = rank;
                // Second stage. Sweep the dominated points.
                int next = ++curr;
                while (next < last) {
                    int nextI = indices[next];
                    if (DominanceHelper.strictlyDominates(currP, points[nextI], dim)) {
                        indices[next] = indices[--last];
                        indices[last] = nextI;
                    } else {
                        ++next;
                    }
                }
            }
            from = last;
        }
    }
}
