package ru.ifmo.nds.deductive;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.DominanceHelper;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

public final class OriginalV3 extends NonDominatedSorting {
    private int[] order;
    private final boolean shuffle;

    public OriginalV3(int maximumPoints, int maximumDimension, boolean shuffle) {
        super(maximumPoints, maximumDimension);
        this.order = new int[maximumPoints];
        this.shuffle = shuffle;
    }

    @Override
    public String getName() {
        return "Deductive Sort, original version 3" + (shuffle ? ", shuffled" : "");
    }

    @Override
    protected void closeImpl() {
        order = null;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        final int n = points.length;
        final int dim = points[0].length;
        Arrays.fill(ranks, 0);
        int[] optimisticLoopState = new int[3];
        // First, we run the optimistic part which assumes all ranks to be zero.
        if (foundAnyDominance(points, n, dim, optimisticLoopState)) {
            // The optimism has failed, need to do the real job.
            // `unrankedNext` is the singly-linked list of unranked points in the traversal order.
            final int[] unrankedNext = this.indices;
            initializeNext0(unrankedNext, n);

            // We need to finish the rank-0 iteration first. It features quite strange loops.
            finishRank0(points, ranks, dim, unrankedNext, optimisticLoopState);

            // The `order` array contains all not yet ranked points in the order to be traversed.
            // Now fill the `order` array, in a special way as rank-0 has all the points in [0;n).
            int aliveN = fillOrder0(n, ranks, order);
            // If the version we want requires shuffling, we do it now.
            if (shuffle) {
                shuffleOrder(order, aliveN);
            }
            for (int currRank = 1; currRank <= maximalMeaningfulRank && aliveN > 0; ++currRank) {
                // Initialize the list by the elements stored in `order`.
                initializeNext(unrankedNext, order, aliveN);
                // Perform the complete iteration on these elements.
                normalIteration(points, ranks, dim, unrankedNext, order[0]);
                // Filter the unranked elements, preserving their order.
                aliveN = fillOrder(aliveN, ranks, currRank, order);
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

    private static boolean foundAnyDominance(double[][] points, int n, int dim, int[] state) {
        for (int i = 0; i < n; ++i) {
            double[] currP = points[i];
            for (int j = i; ++j < n; ) {
                int comparison = DominanceHelper.dominanceComparison(currP, points[j], dim);
                if (comparison != 0) {
                    state[0] = i;
                    state[1] = j;
                    state[2] = comparison;
                    return true;
                }
            }
        }
        return false;
    }

    private static void innermostLoop(double[][] points, int[] ranks, int dim, int[] next, int currLeft, int prevRight) {
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

    private static void normalIteration(double[][] points, int[] ranks, int dim, int[] next, int firstPoint) {
        while (firstPoint >= 0) {
            innermostLoop(points, ranks, dim, next, firstPoint, firstPoint);
            firstPoint = next[firstPoint];
        }
    }

    private static void finishRank0(double[][] points, int[] ranks, int dim, int[] next, int[] optimisticLoopState) {
        int lastLeft = optimisticLoopState[0];
        int lastRight = optimisticLoopState[1];
        int lastComparison = optimisticLoopState[2];

        if (lastComparison < 0) {
            ranks[lastRight] = 1;
            next[lastRight - 1] = next[lastRight];
            innermostLoop(points, ranks, dim, next, lastLeft, lastRight - 1);
        } else {
            ranks[lastLeft] = 1;
        }
        normalIteration(points, ranks, dim, next, next[lastLeft]);
    }

    private static void initializeNext0(int[] next, int n) {
        for (int i = 1; i < n; ++i) {
            next[i - 1] = i;
        }
        next[n - 1] = -1;
    }

    private static void initializeNext(int[] next, int[] order, int n) {
        for (int i = 1; i < n; ++i) {
            next[order[i - 1]] = order[i];
        }
        next[order[n - 1]] = -1;
    }

    private static int fillOrder0(int n, int[] ranks, int[] order) {
        int newN = 0;
        for (int i = 0; i < n; ++i) {
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
