package ru.ifmo.nds.deductive;

import java.util.Arrays;

import ru.ifmo.nds.NonDominatedSorting;

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
        state.solve(points, ranks, maximalMeaningfulRank);
    }

    private static final class State {
        private final double[] flatPoints;
        private double[][] points;
        private final int[] order;
        private int[] ranks;
        private int n, dim, dim2log, maximalMeaningfulRank;
        private int left, grave;

        State(int[] order, int maximumDimension) {
            this.order = order;
            int maxDim2 = 1;
            while (maxDim2 < maximumDimension) {
                maxDim2 += maxDim2;
            }
            this.flatPoints = new double[order.length * maxDim2];
        }

        void solve(double[][] points, int[] ranks, int maximalMeaningfulRank) {
            this.ranks = ranks;
            this.points = points;
            this.maximalMeaningfulRank = maximalMeaningfulRank;
            this.n = points.length;
            this.dim = points[0].length;
            dim2log = 0;
            while ((1 << dim2log) < dim) {
                ++dim2log;
            }

            OptimisticComparator optimisticComparator = new OptimisticComparator();
            this.left = optimisticComparator.getLeftIndex();
            if (optimisticComparator.run(points)) {
                solveRemaining(optimisticComparator.getRightIndex(), optimisticComparator.getComparisonResult());
            }
            Arrays.fill(ranks, 0, optimisticComparator.getLeftIndex(), 0);

            this.ranks = null;
            this.points = null;
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
            return dominanceComparisonImpl(flatPoints, p1, p1 + dim, p2);
        }

        private void solveRemaining(int lastRight, int lastComparison) {
            fillOrderInitially(lastRight);

            int leftI = lastComparison < 0 ? left : lastRight;
            grave = n - 1;
            order[grave] = (left ^ lastRight ^ leftI) << dim2log;
            pointScan0(lastRight, leftI);
            continueSolving();
        }

        private void fillOrderInitially(int lastRight) {
            for (int i = left; i < lastRight; ++i) {
                order[i] = i << dim2log;
            }
            for (int i = left; i < n; ++i) {
                System.arraycopy(points[i], 0, flatPoints, i << dim2log, dim);
            }
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
                    ranks[order[left] >> dim2log] = rankToFill;
                    ++left;
                }
            }
        }

        private void pointScan0(int right, int currI) {
            int rescanUntil = currI;
            currI <<= dim2log;
            int nextI = grave;
            while (grave > right) {
                nextI <<= dim2log;
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
            ranks[currI >> dim2log] = 0;
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
            ranks[currI >> dim2log] = rank;
            rescan(currI, rescanUntil);
        }

        private boolean strictlyDominatesAssumingNotEqual(int p1, int p2) {
            int p1Max = p1 + dim;
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
