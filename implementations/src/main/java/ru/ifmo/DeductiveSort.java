package ru.ifmo;

public class DeductiveSort {
    private static final NonDominatedSortingFactory INSTANCE = (maximumPoints, maximumDimension) ->
            new NonDominatedSorting(maximumPoints, maximumDimension) {
        private int[] indices = new int[maximumPoints];
        private boolean[] dominated = new boolean[maximumPoints];

        @Override
        public String getName() {
            return "Deductive Sort";
        }

        @Override
        protected void closeImpl() throws Exception {
            indices = new int[maximumPoints];
            dominated = new boolean[maximumDimension];
        }

        private int compare(double[] a, double[] b) {
            int d = a.length;
            boolean hasLess = false, hasGreater = false;
            for (int i = 0; i < d; ++i) {
                hasLess |= a[i] < b[i];
                hasGreater |= a[i] > b[i];
                if (hasLess && hasGreater) {
                    return 0;
                }
            }
            return hasLess ? -1 : hasGreater ? 1 : 0;
        }

        @Override
        protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
            int n = points.length;
            for (int i = 0; i < n; ++i) {
                indices[i] = i;
                dominated[i] = false;
            }
            int from = 0;
            for (int rank = 0; from < n; ++rank) {
                for (int i = from; i < n; ++i) {
                    dominated[indices[i]] = false;
                }
                int curr = from;
                int last = n;
                while (curr < last) {
                    int currI = indices[curr];
                    if (dominated[currI]) {
                        throw new AssertionError();
                    }
                    double[] currP = points[currI];
                    int next = curr + 1;
                    while (next < last) {
                        int nextI = indices[next];
                        if (dominated[nextI]) {
                            throw new AssertionError();
                        }
                        double[] nextP = points[nextI];
                        int comparison = compare(currP, nextP);
                        if (comparison < 0) {
                            dominated[nextI] = true;
                            int tmp = indices[--last];
                            indices[last] = indices[next];
                            indices[next] = tmp;
                        } else if (comparison > 0) {
                            dominated[currI] = true;
                            int tmp = indices[--last];
                            indices[last] = indices[curr];
                            indices[curr] = tmp;
                            break;
                        } else {
                            ++next;
                        }
                    }
                    if (!dominated[currI]) {
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
