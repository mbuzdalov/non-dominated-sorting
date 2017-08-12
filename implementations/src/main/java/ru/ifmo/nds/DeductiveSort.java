package ru.ifmo.nds;

public class DeductiveSort {
    private DeductiveSort() {}

    private static final NonDominatedSortingFactory INSTANCE = (maximumPoints, maximumDimension) ->
            new NonDominatedSorting(maximumPoints, maximumDimension) {
        private int[] indices = new int[maximumPoints];

        @Override
        public String getName() {
            return "Deductive Sort";
        }

        @Override
        protected void closeImpl() throws Exception {
            indices = null;
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
            }
            int from = 0;
            for (int rank = 0; from < n; ++rank) {
                int curr = from;
                int last = n;
                while (curr < last) {
                    int currI = indices[curr];
                    double[] currP = points[currI];
                    int next = curr + 1;
                    boolean currentDominated = false;
                    while (next < last) {
                        int nextI = indices[next];
                        double[] nextP = points[nextI];
                        int comparison = compare(currP, nextP);
                        if (comparison < 0) {
                            int tmp = indices[--last];
                            indices[last] = indices[next];
                            indices[next] = tmp;
                        } else if (comparison > 0) {
                            currentDominated = true;
                            int tmp = indices[--last];
                            indices[last] = indices[curr];
                            indices[curr] = tmp;
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
