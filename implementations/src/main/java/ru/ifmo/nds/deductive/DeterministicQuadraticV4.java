package ru.ifmo.nds.deductive;

import java.util.Arrays;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;

public final class DeterministicQuadraticV4 extends NonDominatedSorting {
    public DeterministicQuadraticV4(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    public String getName() {
        return "Deductive Sort, deterministic worst-case quadratic version, complex implementation #2";
    }

    @Override
    protected void closeImpl() {}

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        State s = new State(points, indices, ranks);
        s.run(points.length, maximalMeaningfulRank + 1);
    }

    private static class State {
        private final double[][] points;
        private final int[] indices;
        private final int[] ranks;
        private final int dim;

        private int from, until;
        private int rank;

        State(double[][] points, int[] indices, int[] ranks) {
            this.points = points;
            this.indices = indices;
            this.ranks = ranks;
            this.dim = points[0].length;
        }

        int dominanceComparison(double[] a, double[] b) {
            return DominanceHelper.dominanceComparison(a, b, dim);
        }

        void run(int n, int nextNonExistingRank) {
            // First, we try to perform a cheap non-dominance check that does not use indices.
            OptimisticComparator optimisticComparator = new OptimisticComparator();
            if (optimisticComparator.hasDominatingPoints(points)) {
                // We are here because one of the points has been dominated.
                // First, we need to continue an interrupted dominance scan.
                from = optimisticComparator.getLeftIndex();
                until = n;
                continue0(optimisticComparator.getRightIndex(), optimisticComparator.getComparisonResult());
                // Second, the current level may be far from being complete.
                // The incomplete part is between from and until.
                // However, if it has been completed, we switch to rank 1 immediately.
                if (from == until) {
                    rank = 1;
                    until = n;
                }
                // Finally, we hand out the computation to the generic algorithm.
                runGenericAlgorithm(n, nextNonExistingRank);
                fillExtraBadPoints(nextNonExistingRank);
            }
            // Ranks of points that were successfully processed by the optimistic comparator are all zeros.
            Arrays.fill(ranks, 0, optimisticComparator.getLeftIndex(), 0);
        }

        private void continue0(int next, int comparisonResult) {
            int nextFrom = next ^ from;
            int currI = comparisonResult < 0 ? from : next;
            indices[--until] = nextFrom ^ currI;
            // The remaining points are subject to permutation.
            // However, indices in [next; until) will be written by finalize0,
            // so we write only the points which are not affected by that procedure.
            ArrayHelper.fillIdentityFromIndex(indices, ++from, next);
            // Continue the interrupted iteration.
            finalize0(currI, next);
        }

        private void finalize0(int currI, int next) {
            double[] currP = points[currI];
            int nextI = until;
            int replayUntil = currI;

            while (next < until) {
                double[] nextP = points[nextI];
                int comparison = dominanceComparison(currP, nextP);
                if (comparison == 0) {
                    // we leave next, so we write its final value
                    indices[next] = nextI;
                    nextI = ++next;
                } else {
                    --until;
                    if (comparison > 0) {
                        indices[until] = currI;
                        currI = nextI;
                        currP = nextP;
                        replayUntil = next;
                    } else {
                        indices[until] = nextI;
                    }
                    nextI = until;
                }
            }

            ranks[currI] = 0;
            replay(currP, replayUntil);
        }

        private void runGenericAlgorithm(int n, int nextNonExistingRank) {
            while (rank < nextNonExistingRank && from < until) {
                while (from < until) {
                    iterateInner();
                }
                from = until;
                until = n;
                ++rank;
            }
        }

        private void fillExtraBadPoints(int nextNonExistingRank) {
            while (from < until) {
                ranks[indices[from]] = nextNonExistingRank;
                ++from;
            }
        }

        private void iterateInner() {
            int currI = indices[from];
            double[] currP = points[currI];
            int next = ++from;
            int replayUntil = next;

            outerLoop:
            while ((next = scanUntilDominance(currP, next)) != until) {
                int comparison = next >> 31;
                next ^= comparison;
                int oldCurrI = currI;
                int nextI = indices[next];
                double[] nextP = points[nextI];

                do {
                    --until;
                    int tailValue;
                    if (comparison < 0) {
                        tailValue = nextI;
                    } else {
                        currP = nextP;
                        tailValue = currI;
                        currI = nextI;
                    }
                    nextI = indices[until];
                    indices[until] = tailValue;
                    if (next == until) {
                        if (currI != oldCurrI) {
                            replayUntil = next;
                        }
                        break outerLoop;
                    }

                    nextP = points[nextI];
                    comparison = dominanceComparison(currP, nextP);
                } while (comparison != 0);

                indices[next] = nextI;
                if (currI != oldCurrI) {
                    replayUntil = next;
                }
                ++next;
            }

            ranks[currI] = rank;
            replay(currP, replayUntil);
        }

        private int scanUntilDominance(double[] currP, int next) {
            while (next < until) {
                int comparison = dominanceComparison(currP, points[indices[next]]);
                if (comparison != 0) {
                    return next ^ (comparison >> 1);
                }
                ++next;
            }
            return next;
        }

        private void replay(double[] currP, int replayUntil) {
            // Everything initially at index cannot be equal to currP and cannot dominate it.
            // This allows using an efficient comparison.
            // By looping from the end, we also simplify the logic vastly.
            while (replayUntil > from) {
                --replayUntil;
                int nextI = indices[replayUntil];
                if (DominanceHelper.strictlyDominatesAssumingNotEqual(currP, points[nextI], dim)) {
                    --until;
                    indices[replayUntil] = indices[until];
                    indices[until] = nextI;
                }
            }
        }
    }
}
