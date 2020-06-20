package ru.ifmo.nds.deductive;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;

import java.util.Arrays;

public final class LibraryV4 extends NonDominatedSorting {
    private int[] currOrder;

    public LibraryV4(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        this.currOrder = new int[maximumPoints];
    }

    @Override
    public String getName() {
        return "Deductive Sort, library version 4";
    }

    @Override
    protected void closeImpl() {
        currOrder = null;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        final int[] order = this.indices;
        final int[] currOrder = this.currOrder;
        final int n = points.length;
        final int dim = points[0].length;

        Arrays.fill(ranks, maximalMeaningfulRank + 1);

        ArrayHelper.fillIdentity(order, n);

        int currRank = 0;
        int nRemaining = n;
        boolean notShuffled = true;
        long comparisonsRemainingToShuffle = (long) n * (n - 1);
        while (nRemaining > 0 && currRank <= maximalMeaningfulRank) {
            if (notShuffled && comparisonsRemainingToShuffle < 0) {
                notShuffled = false;
                ArrayHelper.shuffle(order, nRemaining);
            }
            System.arraycopy(order, 0, currOrder, 0, nRemaining);

            int curr = 0, until = nRemaining;
            while (curr < until) {
                int currI = currOrder[curr];
                double[] currP = points[currI];
                int next = curr + 1;

                // Do a quick scan first; if all the remaining points are incomparable with the current one, this will be cheap
                int comparison = 0;
                int nextI = -1;
                while (next < until && (comparison = DominanceHelper.dominanceComparison(currP, points[nextI = currOrder[next]], dim)) == 0) {
                    ++next;
                }
                comparisonsRemainingToShuffle -= next - curr - 1;

                if (comparison != 0) {
                    // at least some of the points are going to be moved
                    int newUntil = next;
                    int localComparisons = 1;
                    while (true) {
                        if (comparison == 0) {
                            // save the current point
                            currOrder[newUntil] = nextI;
                            ++newUntil;
                        } else if (comparison > 0) {
                            // currP is dominated. Here, we replace currI and currP with nextI and points[nextI]
                            currI = nextI;
                            currP = points[nextI];
                            // We also cleanup the already scanned points with the new currP.
                            // A striking feature is that none of these points dominate currP, and none is equal!
                            newUntil = kickDominatedPoints(currOrder, curr + 1, newUntil, currP, points, dim - 1);
                            comparisonsRemainingToShuffle -= newUntil - curr - 1;
                        }
                        if (++next == until) {
                            break;
                        }
                        ++localComparisons;
                        comparison = DominanceHelper.dominanceComparison(currP, points[nextI = currOrder[next]], dim);
                    }
                    comparisonsRemainingToShuffle -= localComparisons;
                    until = newUntil;
                }
                ranks[currI] = currRank;
                ++curr;
            }

            if (nRemaining != curr) {
                filterHigherRanksAssumingTheyExist(order, ranks, nRemaining, currRank);
            }
            nRemaining -= curr;
            ++currRank;
        }
    }

    private static int kickDominatedPoints(int[] order, int from, int until, double[] point, double[][] points, int maxObj) {
        while (from < until && !DominanceHelper.strictlyDominatesAssumingNotEqual(point, points[order[from]], maxObj)) {
            ++from;
        }
        for (int i = from + 1; i < until; ++i) {
            int ii = order[i];
            if (!DominanceHelper.strictlyDominatesAssumingNotEqual(point, points[ii], maxObj)) {
                order[from] = ii;
                ++from;
            }
        }
        return from;
    }

    private static void filterHigherRanksAssumingTheyExist(int[] order, int[] ranks, int size, int value) {
        int newSize = 0;
        while (ranks[order[newSize]] > value) {
            ++newSize;
        }
        for (int i = newSize + 1; i < size; ++i) {
            int ii = order[i];
            if (ranks[ii] > value) {
                order[newSize] = ii;
                ++newSize;
            }
        }
    }
}
