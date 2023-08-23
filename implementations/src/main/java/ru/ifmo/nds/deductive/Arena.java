package ru.ifmo.nds.deductive;

import java.util.Arrays;

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
        int left, trashStart;

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
            int innerResult = optimisticRun();
            Arrays.fill(ranks, 0, left, 0);

            if (innerResult != 0) {
                solveRemaining(innerResult);
            }

            points = null;
            ranks = null;
        }

        int optimisticRun() {
            left = 0;
            do {
                int innerResult = naiveInner();
                if (innerResult != 0) {
                    return innerResult;
                }
            } while (++left < n);
            return 0;
        }

        int naiveInner() {
            double[] currP = points[left];
            int right = left;
            while (++right < n) {
                int comparison = DominanceHelper.dominanceComparison(currP, points[right], dim);
                if (comparison != 0) {
                    return right ^ (comparison >> 1);
                }
            }
            return 0;
        }

        void solveRemaining(int innerResult) {
            int innerResultSign = innerResult >> 31;
            int lastRight = innerResult ^ innerResultSign;
            ArrayHelper.fillIdentityFromIndex(order, left, lastRight);

            int leftI = innerResultSign < 0 ? left : lastRight;
            trashStart = n - 1;
            order[trashStart] = left ^ lastRight ^ leftI;
            pointScan0(lastRight, leftI);
            continueSolving();
        }

        void continueSolving() {
            int rank = 0;
            ++left;
            do {
                continueInner(rank);
                left = trashStart;
                trashStart = n;
            } while (++rank <= maximalMeaningfulRank && left < trashStart);
            fillNonMeaningfulRank();
        }

        void continueInner(int rank) {
            while (left < trashStart) {
                pointScan(rank);
                ++left;
            }
        }

        void fillNonMeaningfulRank() {
            if (left < n) {
                int rankToFill = maximalMeaningfulRank + 1;
                while (left < n) {
                    ranks[order[left]] = rankToFill;
                    ++left;
                }
            }
        }

        void pointScan0(int right, int currI) {
            int rescanUntil = currI;
            double[] currP = points[currI];
            int nextI = trashStart;
            int nIterations = trashStart - right;
            while (--nIterations >= 0) {
                double[] nextP = points[nextI];
                int comparison = DominanceHelper.dominanceComparison(currP, nextP, dim);
                if (comparison != 0) {
                    int trashed = nextI;
                    if (comparison > 0) {
                        trashed = currI;
                        rescanUntil = right;
                        currI = nextI;
                        currP = nextP;
                    }
                    nextI = --trashStart;
                    order[trashStart] = trashed;
                } else {
                    order[right] = nextI;
                    nextI = ++right;
                }
            }
            order[left] = currI;
            ranks[currI] = 0;
            rescan(currP, rescanUntil);
        }

        void pointScan(int rank) {
            int rescanUntil = left;
            int right = left + 1;
            int currI = order[left];
            double[] currP = points[currI];
            int nIterations = trashStart - right;
            while (--nIterations >= 0) {
                int nextI = order[right];
                double[] nextP = points[nextI];
                int comparison = DominanceHelper.dominanceComparison(currP, nextP, dim);
                if (comparison != 0) {
                    int trashed = nextI;
                    if (comparison > 0) {
                        trashed = currI;
                        rescanUntil = right;
                        currI = nextI;
                        currP = nextP;
                    }
                    order[right] = order[--trashStart];
                    order[trashStart] = trashed;
                } else {
                    ++right;
                }
            }
            order[left] = currI;
            ranks[currI] = rank;
            rescan(currP, rescanUntil);
        }

        void rescan(double[] currP, int right) {
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
