package ru.ifmo.nds.deductive;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;

import java.util.Arrays;

public final class ReorderingV2 extends NonDominatedSorting {
    public ReorderingV2(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    public String getName() {
        return "Deductive Sort, reordering version 2";
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
            outerLoop:
            while (curr < last) {
                final int currI = indices[curr];
                final double[] currP = points[currI];
                int next = curr + 1;
                while (next < last) {
                    final int nextI = indices[next];
                    switch (DominanceHelper.dominanceComparison(currP, points[nextI], dim)) {
                        case -1:
                            indices[next] = indices[--last];
                            indices[last] = nextI;
                            break;
                        case 0:
                            ++next;
                            break;
                        case +1:
                            indices[next] = indices[--last];
                            indices[last] = currI;
                            indices[curr] = nextI;
                            continue outerLoop;
                    }
                }
                ranks[currI] = rank;
                ++curr;
            }
            from = last;
        }
    }
}
