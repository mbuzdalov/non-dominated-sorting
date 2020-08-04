package ru.ifmo.nds.deductive;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;

import java.util.Arrays;

public final class QuadraticFast extends NonDominatedSorting {
    public QuadraticFast(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    public String getName() {
        return "Deductive Sort, deterministic worst-case quadratic version, complicated implementation";
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
            // The remaining points are subject to permutation
            ArrayHelper.fillIdentityFromIndex(indices, from + 1, --until);
            // Set the variables depending on whether curr dominates next or not.
            int next = stopped >= 0 ? stopped : ~stopped;
            indices[next] = until;
            int currI = stopped >= 0 ? from : next;
            replayUntil = currI;
            indices[until] = from ^ next ^ currI;
            // Continue the interrupted iteration.
            currI = iterateInner(currI, next);
            ranks[currI] = 0;
            if (replayUntil > ++from) {
                replay(currI);
            }
            // Continue the current level in the generic way
            iterate();
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

        private void iterate() {
            while (from < until) {
                int currI = iterateInner(indices[from], replayUntil = ++from);
                ranks[currI] = rank;
                if (replayUntil > from) {
                    // The current point got replaced at least once.
                    // This means we need to scan the prefix [curr; replayUntil) once more.
                    replay(currI);
                }
            }
        }

        private int iterateInner(int currI, int next) {
            double[] currP = points[currI];
            while (next < until) {
                int nextI = indices[next];
                int comparison = DominanceHelper.dominanceComparison(currP, points[nextI], dim);
                if (comparison == 0) {
                    ++next;
                } else {
                    indices[next] = indices[--until];
                    if (comparison < 0) {
                        indices[until] = nextI;
                    } else {
                        indices[until] = currI;
                        // Remember to scan the prefix since we updated the current point.
                        replayUntil = next;
                        currI = nextI;
                        currP = points[nextI];
                    }
                }
            }
            return currI;
        }

        private void replay(int currI) {
            final int maxObj = dim - 1;
            final double[] currP = points[currI];
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
