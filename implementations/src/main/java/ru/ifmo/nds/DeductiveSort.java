package ru.ifmo.nds;

import ru.ifmo.nds.util.DominanceHelper;

public final class DeductiveSort {
    private DeductiveSort() {}

    private static final NonDominatedSortingFactory INSTANCE = (maximumPoints, maximumDimension) ->
            new NonDominatedSorting(maximumPoints, maximumDimension) {
        @Override
        public String getName() {
            return "Deductive Sort";
        }

        @Override
        protected void closeImpl() {}

        @Override
        protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
            final int[] indices = this.indices;
            final int n = points.length;
            final int dim = points[0].length;
            for (int i = 0; i < n; ++i) {
                indices[i] = i;
            }
            int from = 0;
            for (int rank = 0; from < n; ++rank) {
                int curr = from;
                int last = n;
                while (curr < last) {
                    final int currI = indices[curr];
                    final double[] currP = points[currI];
                    int next = curr + 1;
                    boolean currentDominated = false;
                    while (next < last) {
                        final int nextI = indices[next];
                        int comparison = DominanceHelper.dominanceComparison(currP, points[nextI], dim);
                        if (comparison < 0) {
                            indices[next] = indices[--last];
                            indices[last] = nextI;
                        } else if (comparison > 0) {
                            currentDominated = true;
                            indices[curr] = indices[--last];
                            indices[last] = currI;
                            break;
                        } else {
                            ++next;
                        }
                    }
                    if (!currentDominated) {
                        ranks[currI] = rank;
                        ++curr;
                    }
                }
                from = last;
            }
        }
    };

    public static NonDominatedSortingFactory getInstance() {
        return INSTANCE;
    }
}
