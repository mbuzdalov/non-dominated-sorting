package ru.ifmo.nds.deductive;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

import ru.ifmo.nds.NonDominatedSorting;

public final class OriginalV3 extends NonDominatedSorting {
    private State state;
    public OriginalV3(int maximumPoints, int maximumDimension, boolean shuffle) {
        super(maximumPoints, maximumDimension);
        this.state = new State(this.indices, maximumDimension, shuffle);
    }

    @Override
    public String getName() {
        return "Deductive Sort, original version 3" + (state.shuffle ? ", shuffled" : "");
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
        final int[] next, order;
        int n, dim, maximalMeaningfulRank;
        final double[] flatPoints;
        int[] ranks;
        final boolean shuffle;

        State(int[] next, int maximumDimension, boolean shuffle) {
            this.next = next;
            this.order = new int[next.length];
            this.flatPoints = new double[maximumDimension * next.length];
            this.shuffle = shuffle;
        }

        void init(double[][] points, int[] ranks, int maximalMeaningfulRank) {
            this.n = points.length;
            this.dim = points[0].length;
            this.ranks = ranks;
            this.maximalMeaningfulRank = maximalMeaningfulRank;
            for (int i = 0, t = 0; i < n; ++i, t += dim) {
                System.arraycopy(points[i], 0, flatPoints, t, dim);
            }
        }

        void solve() {
            Arrays.fill(ranks, 0);

            for (int i = 0; i < n; ++i) {
                if (naiveInner(i)) {
                    break;
                }
            }

            ranks = null;
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

        boolean naiveInner(int left) {
            int lp = left * dim;
            for (int j = left; ++j < n; ) {
                int comparison = dominanceComparison(lp, j * dim);
                if (comparison != 0) {
                    solveRemaining(left, j, comparison);
                    return true;
                }
            }
            return false;
        }

        private void solveRemaining(int lastLeft, int lastRight, int lastComparison) {
            Arrays.fill(ranks, lastLeft, n, -1);
            initializeNext0(next, lastLeft, n);
            completeInterruptedIteration(lastLeft, lastRight, lastComparison);
            normalIteration(next[lastLeft], 0);

            int aliveN = fillOrder0(lastLeft, n, ranks, order);
            // If the version we want requires shuffling, we do it now.
            if (shuffle) {
                shuffleOrder(order, aliveN);
            }
            continueSorting(aliveN);
        }

        private void completeInterruptedIteration(int lastLeft, int lastRight, int lastComparison) {
            if (lastComparison < 0) {
                next[lastRight - 1] = next[lastRight];
                innermostLoop(lastLeft, lastRight - 1, 0);
            }
        }

        private void continueSorting(int aliveN) {
            for (int currRank = 1; currRank <= maximalMeaningfulRank && aliveN > 0; ++currRank) {
                // Initialize the list by the elements stored in `order`.
                initializeNext(next, order, aliveN);
                // Perform the complete iteration on these elements.
                normalIteration(order[0], currRank);
                // Filter the unranked elements, preserving their order.
                aliveN = fillOrder(aliveN, ranks, currRank, order);
            }
            fillRemaining(aliveN, maximalMeaningfulRank + 1);
        }

        private void fillRemaining(int aliveN, int rank) {
            for (int i = 0; i < aliveN; ++i) {
                ranks[order[i]] = rank;
            }
        }

        private void normalIteration(int firstPoint, int rank) {
            while (firstPoint >= 0) {
                innermostLoop(firstPoint, firstPoint, rank);
                firstPoint = next[firstPoint];
            }
        }

        private void innermostLoop(int currLeft, int prevRight, int rank) {
            int clp = currLeft * dim;
            int currRight = next[prevRight];
            while (currRight >= 0) {
                switch (dominanceComparison(clp, currRight * dim)) {
                    case -1:
                        next[prevRight] = currRight = next[currRight];
                        break;
                    case 0:
                        prevRight = currRight;
                        currRight = next[currRight];
                        break;
                    case +1:
                        return;
                }
            }
            ranks[currLeft] = rank;
        }
    }

    private static void shuffleOrder(int[] order, int n) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        for (int i = 1; i < n; ++i) {
            int j = random.nextInt(i + 1);
            if (i != j) {
                int tmp = order[i];
                order[i] = order[j];
                order[j] = tmp;
            }
        }
    }

    private static void initializeNext0(int[] next, int lastLeft, int n) {
        int maxLeft = n - 1;
        do {
            next[lastLeft] = ++lastLeft;
        } while (lastLeft < maxLeft);
        next[maxLeft] = -1;
    }

    private static void initializeNext(int[] next, int[] order, int n) {
        int maxIndex = n - 1;
        int i = 0;
        do {
            next[order[i]] = order[++i];
        } while (i < maxIndex);
        next[order[maxIndex]] = -1;
    }

    private static int fillOrder0(int lastLeft, int n, int[] ranks, int[] order) {
        int newN = 0;
        for (int i = lastLeft; i < n; ++i) {
            if (ranks[i] != 0) {
                order[newN] = i;
                ++newN;
            }
        }
        return newN;
    }

    private static int fillOrder(int n, int[] ranks, int currRank, int[] order) {
        int newN = 0;
        for (int i = 0; i < n; ++i) {
            int value = order[i];
            if (ranks[value] != currRank) {
                order[newN] = value;
                ++newN;
            }
        }
        return newN;
    }
}
