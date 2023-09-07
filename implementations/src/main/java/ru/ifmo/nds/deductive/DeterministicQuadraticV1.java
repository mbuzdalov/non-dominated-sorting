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

        ArrayHelper.fillIdentity(indices, n);
        Arrays.fill(ranks, maximalMeaningfulRank + 1);

        int from = 0;
        for (int rank = 0; from < n && rank <= maximalMeaningfulRank; ++rank) {
            int curr = from;
            int last = n;
            while (curr < last) {
                int currI = indices[curr];
                double[] currP = points[currI];
                // First stage. Scan towards the end, replacing the current point when it gets dominated
                int next = curr + 1;
                int rescanUntil = next;
                while (next < last) {
                    int nextI = indices[next];
                    double[] nextP = points[nextI];
                    switch (DominanceHelper.dominanceComparison(currP, nextP, dim)) {
                        case -1:
                            indices[next] = indices[--last];
                            indices[last] = nextI;
                            break;
                        case 0:
                            ++next;
                            break;
                        default:
                            indices[next] = indices[--last];
                            indices[last] = currI;
                            currI = nextI;
                            currP = nextP;
                            rescanUntil = next;
                            break;
                    }
                }
                ranks[currI] = rank;
                // Second stage. Rescan the points before the last replacement
                while (--rescanUntil > curr) {
                    int nextI = indices[rescanUntil];
                    if (DominanceHelper.strictlyDominatesAssumingNotEqual(currP, points[nextI], dim)) {
                        indices[rescanUntil] = indices[--last];
                        indices[last] = nextI;
                    }
                }
                ++curr;
            }
            from = last;
        }
    }
}
