package ru.ifmo.nds.deductive;

import java.util.Arrays;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;

public final class LibraryV3 extends NonDominatedSorting {
    private int[] next;
    private final boolean shuffle;

    public LibraryV3(int maximumPoints, int maximumDimension, boolean shuffle) {
        super(maximumPoints, maximumDimension);
        this.shuffle = shuffle;
        this.next = new int[maximumPoints];
    }

    @Override
    public String getName() {
        return "Deductive Sort, library version 3, shuffle: " + (shuffle ? "yes" : "no");
    }

    @Override
    protected void closeImpl() {
        next = null;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        final int[] order = this.indices;
        final int n = points.length;
        final int dim = points[0].length;

        Arrays.fill(ranks, maximalMeaningfulRank + 1);

        ArrayHelper.fillIdentity(order, n);

        int currRank = 0;
        int nRemaining = n;
        boolean notShuffled = true;
        long comparisonsRemainingToShuffle = Math.max(100, (long) n * (n - 1) / 2);
        while (nRemaining > 0 && currRank <= maximalMeaningfulRank) {
            if (shuffle && notShuffled && comparisonsRemainingToShuffle < 0) {
                notShuffled = false;
                ArrayHelper.shuffle(order, nRemaining);
            }
            fillNextByOrder(next, order, nRemaining);

            for (int point = order[0]; point >= 0; point = next[point]) {
                final double[] currP = points[point];
                int prev = point;
                int curr = next[point];
                boolean nonDominated = true;
                int localComparisons = 0;
                while (curr != -1) {
                    int comparison = DominanceHelper.dominanceComparison(currP, points[curr], dim);
                    ++localComparisons;
                    if (comparison == 0) {
                        prev = curr;
                        curr = next[curr];
                    } else if (comparison < 0) {
                        curr = next[curr];
                        next[prev] = curr;
                    } else {
                        nonDominated = false;
                        break;
                    }
                }
                comparisonsRemainingToShuffle -= localComparisons;
                if (nonDominated) {
                    ranks[point] = currRank;
                }
            }

            nRemaining = filterHigherRanks(order, ranks, nRemaining, currRank);
            ++currRank;
        }
    }

    private static void fillNextByOrder(int[] next, int[] order, int nRemaining) {
        int prev = order[0];
        for (int i = 1; i < nRemaining; ++i) {
            int curr = order[i];
            next[prev] = curr;
            prev = curr;
        }
        next[prev] = -1;
    }

    private static int filterHigherRanks(int[] order, int[] ranks, int size, int value) {
        int newSize = 0;
        for (int i = 0; i < size; ++i) {
            int ii = order[i];
            if (ranks[ii] > value) {
                order[newSize] = ii;
                ++newSize;
            }
        }
        return newSize;
    }
}
