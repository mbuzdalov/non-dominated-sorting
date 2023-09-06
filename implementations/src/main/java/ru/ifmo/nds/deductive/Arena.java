package ru.ifmo.nds.deductive;

import java.util.Arrays;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;

public final class Arena extends NonDominatedSorting {
    private State state;

    public Arena(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        state = new State(indices, maximumDimension);
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
        private final double[] flatPoints;
        private final int[] order;
        private int[] ranks;
        private int n, dim, maximalMeaningfulRank;
        private int left, grave;

        State(int[] order, int maximumDimension) {
            this.order = order;
            this.flatPoints = new double[order.length * maximumDimension];
        }

        void init(double[][] points, int[] ranks, int maximalMeaningfulRank) {
            this.ranks = ranks;
            this.maximalMeaningfulRank = maximalMeaningfulRank;
            this.n = points.length;
            this.dim = points[0].length;
            for (int i = 0, t = 0; i < n; ++i, t += dim) {
                System.arraycopy(points[i], 0, flatPoints, t, dim);
            }
        }

        void solve() {
            int innerResult = optimisticRun();
            Arrays.fill(ranks, 0, left, 0);

            if (innerResult != 0) {
                solveRemaining(innerResult);
            }

            ranks = null;
        }

        private int optimisticRun() {
            left = 0;
            do {
                int innerResult = naiveInner();
                if (innerResult != 0) {
                    return innerResult;
                }
            } while (++left < n);
            return 0;
        }

        private static int dominanceComparisonLess(double[] flatPoints, int p1, int p1Max, int p2) {
            while (++p1 < p1Max) {
                if (flatPoints[p1] > flatPoints[++p2]) {
                    return 0;
                }
            }
            return -1;
        }

        private static int dominanceComparisonGreater(double[] flatPoints, int p1, int p1Max, int p2) {
            while (++p1 < p1Max) {
                if (flatPoints[p1] < flatPoints[++p2]) {
                    return 0;
                }
            }
            return +1;
        }

        private static int dominanceComparisonImpl(double[] flatPoints, int p1, int p1Max, int p2) {
            while (p1 < p1Max) {
                double a = flatPoints[p1];
                double b = flatPoints[p2];
                if (a < b) {
                    return dominanceComparisonLess(flatPoints, p1, p1Max, p2);
                }
                if (a > b) {
                    return dominanceComparisonGreater(flatPoints, p1, p1Max, p2);
                }
                ++p1;
                ++p2;
            }
            return 0;
        }

        private int dominanceComparison(int p1, int p2) {
            p1 *= dim;
            return dominanceComparisonImpl(flatPoints, p1, p1 + dim, p2 * dim);
        }

        private int naiveInner() {
            int right = left;
            while (++right < n) {
                int comparison = dominanceComparison(left, right);
                if (comparison != 0) {
                    return right ^ (comparison >> 1);
                }
            }
            return 0;
        }

        private void solveRemaining(int innerResult) {
            int innerResultSign = innerResult >> 31;
            int lastRight = innerResult ^ innerResultSign;
            ArrayHelper.fillIdentityFromIndex(order, left, lastRight);

            int leftI = innerResultSign < 0 ? left : lastRight;
            grave = n - 1;
            order[grave] = left ^ lastRight ^ leftI;
            pointScan0(lastRight, leftI);
            continueSolving();
        }

        private void continueSolving() {
            int rank = 0;
            ++left;
            do {
                continueInner(rank);
                left = grave;
                grave = n;
            } while (++rank <= maximalMeaningfulRank && left < grave);
            fillNonMeaningfulRank();
        }

        private void continueInner(int rank) {
            while (left < grave) {
                pointScan(rank);
                ++left;
            }
        }

        private void fillNonMeaningfulRank() {
            if (left < n) {
                int rankToFill = maximalMeaningfulRank + 1;
                while (left < n) {
                    ranks[order[left]] = rankToFill;
                    ++left;
                }
            }
        }

        private void pointScan0(int right, int currI) {
            int rescanUntil = currI;
            int nextI = grave;
            while (grave > right) {
                int comparison = dominanceComparison(currI, nextI);
                if (comparison != 0) {
                    --grave;
                    if (comparison > 0) {
                        order[grave] = currI;
                        rescanUntil = right;
                        currI = nextI;
                    } else {
                        order[grave] = nextI;
                    }
                    nextI = grave;
                } else {
                    order[right] = nextI;
                    nextI = ++right;
                }
            }
            ranks[currI] = 0;
            rescan(currI, rescanUntil);
        }

        private void pointScan(int rank) {
            int rescanUntil = left;
            int right = left + 1;
            int currI = order[left];
            while (grave > right) {
                int nextI = order[right];
                int comparison = dominanceComparison(currI, nextI);
                if (comparison != 0) {
                    --grave;
                    order[right] = order[grave];
                    if (comparison > 0) {
                        order[grave] = currI;
                        rescanUntil = right;
                        currI = nextI;
                    } else {
                        order[grave] = nextI;
                    }
                } else {
                    ++right;
                }
            }
            ranks[currI] = rank;
            rescan(currI, rescanUntil);
        }

        private boolean strictlyDominatesAssumingNotEqual(int p1, int p2) {
            int p1Max = p1 + dim;
            p2 *= dim;
            while (p1 < p1Max) {
                if (flatPoints[p1] > flatPoints[p2]) {
                    return false;
                }
                ++p1;
                ++p2;
            }
            return true;
        }

        private void rescan(int currI, int right) {
            currI *= dim;
            while (--right > left) {
                int nextI = order[right];
                if (strictlyDominatesAssumingNotEqual(currI, nextI)) {
                    --grave;
                    order[right] = order[grave];
                    order[grave] = nextI;
                }
            }
        }
    }
}
