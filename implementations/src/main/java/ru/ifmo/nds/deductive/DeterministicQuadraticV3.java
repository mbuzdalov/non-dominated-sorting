package ru.ifmo.nds.deductive;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;

import java.util.Arrays;

public final class DeterministicQuadraticV3 extends NonDominatedSorting {
    public DeterministicQuadraticV3(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    public String getName() {
        return "Deductive Sort, deterministic worst-case quadratic version, complex implementation";
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

        private int from, replayUntil, until;
        private int rank;

        State(double[][] points, int[] indices, int[] ranks) {
            this.points = points;
            this.indices = indices;
            this.ranks = ranks;
            this.dim = points[0].length;
        }

        void run(int n, int nextNonExistingRank) {
            rank = 0;
            from = 0;
            until = n;
            iterate0();
            while (until < n && ++rank < nextNonExistingRank) {
                from = until;
                until = n;
                iterate();
            }

            while (until < n) {
                ranks[indices[until]] = nextNonExistingRank;
                ++until;
            }
        }

        private void iterate0() {
            while (from < until) {
                int stopped = iterateInner0();
                if (stopped != until) {
                    continue0(stopped);
                    return;
                }
                ++from;
            }
            Arrays.fill(ranks, 0, until, 0);
        }

        private void continue0(int stopped) {
            // All points from 0 until from are already rank 0
            Arrays.fill(ranks, 0, from, 0);
            // Set the variables depending on whether curr dominates next or not.
            int next = stopped >= 0 ? stopped : ~stopped;
            // The remaining points are subject to permutation.
            // However, indices in [next; until) will be written by iterateInner0Continue,
            // so we write only the points which are not affected by that procedure.
            ArrayHelper.fillIdentityFromIndex(indices, from + 1, next);
            int currI = stopped >= 0 ? from : next;
            replayUntil = currI;
            indices[--until] = from ^ next ^ currI;
            // Continue the interrupted iteration.
            if (next < until) {
                currI = iterateInner0Continue(currI, next);
            }
            ranks[currI] = 0;
            if (replayUntil > ++from) {
                replay(points[currI], replayUntil);
            }
            // Continue the current level in the generic way
            if (from < until) {
                iterate();
            }
        }

        private int iterateInner0() {
            // This is an optimistic loop that thinks no points are going to be dominated.
            double[] currP = points[from];
            for (int next = from + 1; next < until; ++next) {
                int comparison = DominanceHelper.dominanceComparison(currP, points[next], dim);
                if (comparison != 0) {
                    // Our optimism has just been broken, so we break the loop and notify the upstream.
                    return comparison < 0 ? next : ~next;
                }
            }
            return until;
        }

        private int iterateInner0Continue(int currI, int next) {
            double[] currP = points[currI];
            int nextI = until;
            do {
                double[] nextP = points[nextI];
                int comparison = DominanceHelper.dominanceComparison(currP, nextP, dim);
                if (comparison == 0) {
                    // we leave next, so we write its final value
                    indices[next] = nextI;
                    nextI = ++next;
                } else {
                    if (comparison < 0) {
                        indices[--until] = nextI;
                    } else {
                        indices[--until] = currI;
                        replayUntil = next;
                        currI = nextI;
                        currP = nextP;
                    }
                    nextI = until;
                }
            } while (next < until);
            return currI;
        }

        private void iterate() {
            while (true) {
                int currI = indices[from];
                if (++from < until) {
                    iterateInner(currI, from);
                } else {
                    ranks[currI] = rank;
                    return;
                }
            }
        }

        private void iterateInner(int currI, int next) {
            double[] currP = points[currI];
            int replayUntil = next;
            do {
                int nextI = indices[next];
                double[] nextP = points[nextI];
                int comparison = DominanceHelper.dominanceComparison(currP, nextP, dim);
                if (comparison == 0) {
                    ++next;
                } else {
                    indices[next] = indices[--until];
                    if (comparison < 0) {
                        indices[until] = nextI;
                    } else {
                        indices[until] = currI;
                        currI = nextI;
                        currP = nextP;
                        replayUntil = next;
                    }
                }
            } while (next < until);
            ranks[currI] = rank;
            if (replayUntil > from) {
                replay(currP, replayUntil);
            }
        }

        private void replay(double[] currP, int replayUntil) {
            final int maxObj = dim - 1;
            // Everything initially at index cannot be equal to currP and cannot dominate it.
            // This allows using an efficient comparison.
            // When looping from the end, we also simplify the logic vastly.
            while (--replayUntil >= from) {
                int nextI = indices[replayUntil];
                if (DominanceHelper.strictlyDominatesAssumingNotEqual(currP, points[nextI], maxObj)) {
                    indices[replayUntil] = indices[--until];
                    indices[until] = nextI;
                }
            }
        }
    }
}
