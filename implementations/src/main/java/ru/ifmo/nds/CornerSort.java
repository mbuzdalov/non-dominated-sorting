package ru.ifmo.nds;

public class CornerSort {
    private CornerSort() {}

    private static final NonDominatedSortingFactory INSTANCE = (maximumPoints, maximumDimension) ->
            new NonDominatedSorting(maximumPoints, maximumDimension) {
        private int[] indices = new int[maximumPoints];

        @Override
        public String getName() {
            return "Corner Sort";
        }

        @Override
        protected void closeImpl() throws Exception {
            indices = null;
        }

        private boolean dominates(double[] a, double[] b) {
            int d = a.length;
            boolean hasLess = false;
            for (int i = 0; i < d; ++i) {
                hasLess |= a[i] < b[i];
                if (a[i] > b[i]) {
                    return false;
                }
            }
            return hasLess;
        }

        @Override
        protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
            int n = points.length, d = points[0].length;
            for (int i = 0; i < n; ++i) {
                indices[i] = i;
            }
            int from = 0;
            for (int rank = 0, objective = 0; from < n; ++rank) {
                int curr = from;
                int last = n;
                while (curr < last) {
                    int best = curr;
                    double[] bestPoint = points[indices[best]];
                    for (int i = curr + 1; i < last; ++i) {
                        double[] currPoint = points[indices[i]];
                        if (bestPoint[objective] > currPoint[objective] ||
                                bestPoint[objective] == currPoint[objective] && dominates(currPoint, bestPoint)) {
                            bestPoint = currPoint;
                            best = i;
                        }
                    }

                    int tmp = indices[best];
                    indices[best] = indices[curr];
                    indices[curr] = tmp;

                    if (points[tmp] != bestPoint) {
                        throw new AssertionError();
                    }
                    ranks[tmp] = rank;
                    ++curr;

                    while (curr < last) {
                        int ci = indices[curr];
                        double[] currPoint = points[ci];
                        if (dominates(bestPoint, currPoint)) {
                            int li = indices[--last];
                            indices[last] = ci;
                            indices[curr] = li;
                        } else {
                            ++curr;
                        }
                    }
                    curr = ++from;
                    objective = (objective + 1) % d;
                }
                from = last;
            }
        }
    };

    public static NonDominatedSortingFactory getInstance() {
        return INSTANCE;
    }
}
