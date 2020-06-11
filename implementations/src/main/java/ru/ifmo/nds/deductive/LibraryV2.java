package ru.ifmo.nds.deductive;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;

import java.util.Arrays;

public final class LibraryV2 extends NonDominatedSorting {
    public LibraryV2(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    public String getName() {
        return "Deductive Sort, library version 2";
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
                final int currI = indices[curr];
                final double[] currP = points[currI];
                int next = curr + 1;
                boolean nonDominated = true;
                while (next < last) {
                    final int nextI = indices[next];
                    int comparison = DominanceHelper.dominanceComparison(currP, points[nextI], dim);
                    if (comparison == 0) {
                        ++next;
                    } else {
                        indices[next] = indices[--last];
                        if (comparison < 0) {
                            indices[last] = nextI;
                        } else {
                            nonDominated = false;
                            indices[last] = currI;
                            indices[curr] = nextI;
                            break;
                        }
                    }
                }
                if (nonDominated) {
                    ranks[currI] = rank;
                    ++curr;
                }
            }
            from = last;
        }
    }
}
