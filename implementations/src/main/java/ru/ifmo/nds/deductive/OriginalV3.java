package ru.ifmo.nds.deductive;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.DominanceHelper;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public final class OriginalV3 extends NonDominatedSorting {
    private State state;
    public OriginalV3(int maximumPoints, int maximumDimension, boolean shuffle) {
        super(maximumPoints, maximumDimension);
        this.state = new State(this.indices, shuffle);
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
        double[][] points;
        int[] ranks;
        final boolean shuffle;

        State(int[] next, boolean shuffle) {
            this.next = next;
            this.order = new int[next.length];
            this.shuffle = shuffle;
        }

        void init(double[][] points, int[] ranks, int maximalMeaningfulRank) {
            this.n = points.length;
            this.dim = points[0].length;
            this.points = points;
            this.ranks = ranks;
            this.maximalMeaningfulRank = maximalMeaningfulRank;
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

        private void solveRemaining(int lastLeft, int lastRight, int lastComparison) {
            initializeNext0(next, lastLeft, n);
            completeInterruptedIteration(lastLeft, lastRight, lastComparison);
            normalIteration(next[lastLeft]);

            int aliveN = fillOrder0(lastLeft, n, ranks, order);
            // If the version we want requires shuffling, we do it now.
            if (shuffle) {
                shuffleOrder(order, aliveN);
            }
            continueSorting(aliveN);
        }

        private void completeInterruptedIteration(int lastLeft, int lastRight, int lastComparison) {
            if (lastComparison < 0) {
                ranks[lastRight] = 1;
                next[lastRight - 1] = next[lastRight];
                innermostLoop(lastLeft, lastRight - 1);
            } else {
                ranks[lastLeft] = 1;
            }
        }

        private void continueSorting(int aliveN) {
            for (int currRank = 1; currRank <= maximalMeaningfulRank && aliveN > 0; ++currRank) {
                // Initialize the list by the elements stored in `order`.
                initializeNext(next, order, aliveN);
                // Perform the complete iteration on these elements.
                normalIteration(order[0]);
                // Filter the unranked elements, preserving their order.
                aliveN = fillOrder(aliveN, ranks, currRank, order);
            }
        }

        private void normalIteration(int firstPoint) {
            while (firstPoint >= 0) {
                innermostLoop(firstPoint, firstPoint);
                firstPoint = next[firstPoint];
            }
        }

        private void innermostLoop(int currLeft, int prevRight) {
            double[] currP = points[currLeft];
            int currRight = next[prevRight];
            while (currRight >= 0) {
                switch (DominanceHelper.dominanceComparison(currP, points[currRight], dim)) {
                    case -1:
                        ++ranks[currRight];
                        next[prevRight] = currRight = next[currRight];
                        break;
                    case 0:
                        prevRight = currRight;
                        currRight = next[currRight];
                        break;
                    case +1:
                        ++ranks[currLeft];
                        return;
                }
            }
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
            if (ranks[value] > currRank) {
                order[newN] = value;
                ++newN;
            }
        }
        return newN;
    }
}
