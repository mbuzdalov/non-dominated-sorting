package ru.ifmo.nds.deductive;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;

import java.util.Arrays;

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
            Arrays.fill(ranks, 0);

            for (int i = 0; i < n; ++i) {
                double[] currP = points[i];
                for (int j = i; ++j < n; ) {
                    int comparison = DominanceHelper.dominanceComparison(currP, points[j], dim);
                    if (comparison != 0) {
                        solveRemaining(i, j, comparison);
                        return;
                    }
                }
            }

            points = null;
            ranks = null;
        }

        void solveRemaining(int lastLeft, int lastRight, int lastComparison) {
            ArrayHelper.fillIdentityFromIndex(order, lastLeft, n);
            trashStart = n;
            if (lastComparison < 0) {
                ranks[lastRight] = 1;
                order[lastRight] = order[--trashStart];
                order[trashStart] = lastRight;
            } else {
                ranks[lastLeft] = 1;
                order[lastLeft] = lastRight;
                order[lastRight] = order[--trashStart];
                order[trashStart] = lastLeft;
            }
            pointScan(lastLeft, lastRight, order[lastLeft]);
            continueSolving(lastLeft + 1);
        }

        void continueSolving(int intervalStart) {
            int rank = 0;
            do {
                for (int left = intervalStart; left < trashStart; ++left) {
                    pointScan(left, left + 1, left);
                }
                intervalStart = trashStart;
                trashStart = n;
            } while (++rank <= maximalMeaningfulRank && intervalStart < trashStart);
        }

        void pointScan(int left, int right, int rescanMax) {
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
            rescan(currP, left, rescanMax);
        }

        void rescan(double[] currP, int left, int right) {
            while (--right > left) {
                int nextI = order[right];
                if (DominanceHelper.strictlyDominatesAssumingNotEqual(currP, points[nextI], dim)) {
                    ++ranks[nextI];
                    order[right] = order[--trashStart];
                    order[trashStart] = nextI;
                }
            }
        }
    }
}
