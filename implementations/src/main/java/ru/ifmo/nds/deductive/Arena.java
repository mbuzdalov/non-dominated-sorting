package ru.ifmo.nds.deductive;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;

public final class Arena extends NonDominatedSorting {
    private State state;

    public Arena(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        state = new State(indices);
    }

    @Override
    public String getName() {
        return "Deductive Sort, quadratic version (Arena principle)";
    }

    @Override
    protected void closeImpl() {
        state = null;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        state.init(points, ranks, maximalMeaningfulRank);
        state.solve();
    }

    private static final class State {
        double[][] points;
        int[] order, ranks;
        int n, dim, maximalMeaningfulRank;
        int trashStart;

        State(int[] order) {
            this.order = order;
        }

        void init(double[][] points, int[] ranks, int maximalMeaningfulRank) {
            this.points = points;
            this.ranks = ranks;
            this.maximalMeaningfulRank = maximalMeaningfulRank;
            this.n = points.length;
            this.dim = points[0].length;
        }

        void solve() {
            for (int i = 0; i < n; ++i) {
                if (naiveInner(i)) {
                    break;
                }
                ranks[i] = 0;
            }

            points = null;
            ranks = null;
        }

        boolean naiveInner(int left) {
            double[] currP = points[left];
            for (int j = left; ++j < n; ) {
                int comparison = DominanceHelper.dominanceComparison(currP, points[j], dim);
                if (comparison != 0) {
                    solveRemaining(left, j, comparison);
                    return true;
                }
            }
            return false;
        }

        void solveRemaining(int lastLeft, int lastRight, int lastComparison) {
            ArrayHelper.fillIdentityFromIndex(order, lastLeft, n);
            trashStart = n;
            if (lastComparison < 0) {
                order[lastRight] = order[--trashStart];
                order[trashStart] = lastRight;
            } else {
                order[lastLeft] = lastRight;
                order[lastRight] = order[--trashStart];
                order[trashStart] = lastLeft;
            }
            pointScan(0, lastLeft, lastRight, order[lastLeft]);
            continueSolving(lastLeft + 1);
        }

        void continueSolving(int intervalStart) {
            int rank = 0;
            do {
                continueInner(rank, intervalStart);
                intervalStart = trashStart;
                trashStart = n;
            } while (++rank <= maximalMeaningfulRank && intervalStart < trashStart);
            fillNextRank(intervalStart);
        }

        void continueInner(int rank, int intervalStart) {
            for (int left = intervalStart; left < trashStart; ++left) {
                pointScan(rank, left, left + 1, left);
            }
        }

        void fillNextRank(int intervalStart) {
            if (intervalStart < n) {
                int rankToFill = maximalMeaningfulRank + 1;
                while (intervalStart < n) {
                    ranks[order[intervalStart]] = rankToFill;
                    ++intervalStart;
                }
            }
        }

        void pointScan(int rank, int left, int right, int rescanMax) {
            int currI = order[left];
            double[] currP = points[currI];
            while (right < trashStart) {
                int nextI = order[right];
                double[] nextP = points[nextI];
                int comparison = DominanceHelper.dominanceComparison(currP, nextP, dim);
                if (comparison != 0) {
                    if (comparison < 0) {
                        ++ranks[nextI];
                        order[right] = order[--trashStart];
                        order[trashStart] = nextI;
                    } else {
                        ++ranks[currI];
                        order[left] = nextI;
                        order[right] = order[--trashStart];
                        order[trashStart] = currI;
                        rescanMax = right;
                        currI = nextI;
                        currP = nextP;
                    }
                } else {
                    ++right;
                }
            }
            ranks[currI] = rank;
            rescan(currP, left, rescanMax);
        }

        void rescan(double[] currP, int left, int right) {
            while (--right > left) {
                int nextI = order[right];
                if (DominanceHelper.strictlyDominatesAssumingNotEqual(currP, points[nextI], dim)) {
                    order[right] = order[--trashStart];
                    order[trashStart] = nextI;
                }
            }
        }
    }
}
