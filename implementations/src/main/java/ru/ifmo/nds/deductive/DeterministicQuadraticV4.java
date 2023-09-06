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
            until = n;
            // First, we try to perform a cheap non-dominance check that does not use indices.
            int cheapRunResult = iterate0();
            if (cheapRunResult == n) {
                // Hooray, no points have been dominated, all points have rank 0.
                Arrays.fill(ranks, 0, n, 0);
            } else {
                // We are here because one of the points has been dominated.
                // First, we need to continue an interrupted dominance scan.
                continue0(cheapRunResult);
                // Second, the current level may be far from being complete.
                // The incomplete part is between from and until.
                // However, if it has been completed, we switch to rank 1 immediately.
                if (from == until) {
                    rank = 1;
                    until = n;
                }
                // Finally, we hand out the computation to the generic algorithm.
                runGenericAlgorithm(n, nextNonExistingRank);
            }
        }

        private int iterate0() {
            // This is an optimistic loop that thinks no points are going to be dominated.
            do {
                // Inlining this method here would increase the running time by roughly 30%.
                // Tested on openjdk-8.265.
                int result = iterate0Inner();
                if (result != until) {
                    return result;
                }
            } while (++from < until);
            return until;
        }

        private int iterate0Inner() {
            double[] currP = points[from];
            // This is an optimistic loop that thinks no points are going to be dominated.
            int next = from;
            while (++next < until) {
                int comparison = dominanceComparison(currP, points[next]);
                if (comparison != 0) {
                    // Our optimistic assumption has just been broken,
                    // so we break the loop and notify the upstream.
                    return next ^ (comparison >> 1);
                }
            }
            return until;
        }

        private void continue0(int next0) {
            // All points from 0 until from are already rank 0
            Arrays.fill(ranks, 0, from, 0);
            int next = next0 ^ (next0 >> 31);
            int nextFrom = next ^ from;
            int currI = next ^ ((next ^ next0) & nextFrom);
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
            while (true) {
                iterate();
                if (until == n) {
                    break;
                }
                if (++rank == nextNonExistingRank) {
                    fillExtraBadPoints(n, nextNonExistingRank);
                    break;
                }
                from = until;
                until = n;
            }
        }

        private void fillExtraBadPoints(int n, int nextNonExistingRank) {
            do {
                ranks[indices[until]] = nextNonExistingRank;
            } while (++until < n);
        }

        private void iterate() {
            do {
                iterateInner();
            } while (from < until);
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
