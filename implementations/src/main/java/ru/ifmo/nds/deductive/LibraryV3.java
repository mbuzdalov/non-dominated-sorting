package ru.ifmo.nds.deductive;

import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

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
        final int n = points.length;
        final int dim = points[0].length;

        Arrays.fill(ranks, maximalMeaningfulRank + 1);
        ArrayHelper.fillIdentity(indices, n);
        if (shuffle) {
            ThreadLocalRandom random = ThreadLocalRandom.current();
            for (int i = 1; i < n; ++i) {
                int j = random.nextInt(i + 1);
                int tmp = indices[i];
                indices[i] = indices[j];
                indices[j] = tmp;
            }
        }

        int currRank = 0;
        int nRemaining = n;
        while (nRemaining > 0 && currRank <= maximalMeaningfulRank) {
            initLinkedList(next, nRemaining);

            for (int point = 0; point >= 0; point = next[point]) {
                final int currI = indices[point];
                final double[] currP = points[currI];
                int prev = point;
                int curr = next[prev];
                boolean nonDominated = true;
                while (curr != -1) {
                    int comparison = DominanceHelper.dominanceComparison(currP, points[indices[curr]], dim);
                    if (comparison < 0) {
                        curr = next[curr];
                        next[prev] = curr;
                    } else if (comparison > 0) {
                        nonDominated = false;
                        break;
                    } else {
                        prev = curr;
                        curr = next[curr];
                    }
                }
                if (nonDominated) {
                    ranks[currI] = currRank;
                }
            }

            nRemaining = filterHigherRanks(indices, ranks, nRemaining, currRank);
            ++currRank;
        }
    }

    private static void initLinkedList(int[] list, int size) {
        for (int i = 1; i < size; ++i) {
            list[i - 1] = i;
        }
        list[size - 1] = -1;
    }

    private static int filterHigherRanks(int[] indices, int[] ranks, int size, int value) {
        int newSize = 0;
        for (int i = 0; i < size; ++i) {
            int ii = indices[i];
            if (ranks[ii] > value) {
                indices[newSize] = ii;
                ++newSize;
            }
        }
        return newSize;
    }
}
