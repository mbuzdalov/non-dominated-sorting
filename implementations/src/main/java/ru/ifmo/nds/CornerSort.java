package ru.ifmo.nds;

import ru.ifmo.nds.util.ArrayHelper;
import static ru.ifmo.nds.util.DominanceHelper.*;

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
        protected void closeImpl() {
            indices = null;
        }

        private int findBestPoint(double[][] points, int dim, int from, int until, int objective) {
            int best = from;
            double[] bestPoint = points[indices[best]];
            for (int i = from + 1; i < until; ++i) {
                double[] currPoint = points[indices[i]];
                if (bestPoint[objective] > currPoint[objective] ||
                        bestPoint[objective] == currPoint[objective] && strictlyDominates(currPoint, bestPoint, dim)) {
                    bestPoint = currPoint;
                    best = i;
                }
            }
            return best;
        }

        private int moveDominatedToTail(double[][] points, int dim, int index, int until) {
            double[] bestPoint = points[indices[index]];
            ++index;
            while (index < until) {
                if (strictlyDominates(bestPoint, points[indices[index]], dim)) {
                    ArrayHelper.swap(indices, --until, index);
                } else {
                    ++index;
                }
            }
            return until;
        }

        // Note: We have a local "objective" variable.
        // This is not the same as the original version, where "objective" is shared between ranks.
        // However, this does not influence the worst case and shall not influence the average case.

        private int peelCurrentRank(double[][] points, int[] ranks, int dim, int from, int until, int rank) {
            int curr = from;
            int last = until;
            int objective = 0;
            int d = points[0].length;

            while (curr < last) {
                int best = findBestPoint(points, dim, curr, last, objective);
                ArrayHelper.swap(indices, best, curr);
                ranks[indices[curr]] = rank;

                last = moveDominatedToTail(points, dim, curr, last);
                curr = ++from;
                objective = (objective + 1) % d;
            }

            return last;
        }

        private void fillNonMeaningfulRanks(int[] ranks, int from, int until, int maximalMeaningfulRank) {
            for (int i = from; i < until; ++i) {
                ranks[indices[i]] = maximalMeaningfulRank + 1;
            }
        }

        @Override
        protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
            int n = points.length;
            int dim = points[0].length;
            ArrayHelper.fillIdentity(indices, n);
            int firstNotYetProcessed = 0;
            for (int rank = 0; firstNotYetProcessed < n && rank <= maximalMeaningfulRank; ++rank) {
                firstNotYetProcessed = peelCurrentRank(points, ranks, dim, firstNotYetProcessed, n, rank);
            }
            fillNonMeaningfulRanks(ranks, firstNotYetProcessed, n, maximalMeaningfulRank);
        }
    };

    public static NonDominatedSortingFactory getInstance() {
        return INSTANCE;
    }
}
