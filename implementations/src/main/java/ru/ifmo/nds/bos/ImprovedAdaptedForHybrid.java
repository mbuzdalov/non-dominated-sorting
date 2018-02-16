package ru.ifmo.nds.bos;

import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;

import java.util.Arrays;

public class ImprovedAdaptedForHybrid extends AbstractImproved {
    private boolean[] isRanked;
    private double[][] tempPoints;
    private int[] tempRanks;

    public ImprovedAdaptedForHybrid(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        isRanked = new boolean[maximumPoints];
        tempPoints = new double[maximumPoints][maximumDimension];
        tempRanks = new int[maximumPoints];
    }

    @Override
    public String getName() {
        return "Best Order Sort (improved adapter for hybrid implementation)";
    }

    @Override
    protected void closeImpl() {
        super.closeImpl();
        isRanked = null;
        tempPoints = null;
        tempRanks = null;
    }

    private void rankPoint(int currIndex, int[] prevFI, int[] lastFI, int smallestRank, int maximalMeaningfulRank, int M) {
        int currRank = Math.max(smallestRank, ranks[currIndex]);

        // This is currently implemented as sequential search.
        // A binary search implementation is expected as well.
        while (currRank <= maximalMeaningfulRank) {
            int prevIndex = lastFI[currRank];
            boolean someoneDominatesMe = false;
            while (prevIndex != -1) {
                if (dominates(prevIndex, currIndex, M)) {
                    someoneDominatesMe = true;
                    break;
                } else {
                    prevIndex = prevFI[prevIndex];
                }
            }
            if (!someoneDominatesMe) {
                break;
            }
            ++currRank;
        }

        this.ranks[currIndex] = Math.max(this.ranks[currIndex], currRank);
        this.isRanked[currIndex] = true;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        Arrays.fill(ranks, 0, ranks.length, -1);
        sortCheckedWithRespectToRanks(points, ranks, ranks.length, points[0].length, maximalMeaningfulRank);
    }

    @Override
    protected void sortCheckedWithRespectToRanks(double[][] points, int[] ranks, int origN, int dim, int maximalMeaningfulRank) {
        ArrayHelper.fillIdentity(reindex, origN);
        sorter.lexicographicalSort(points, reindex, 0, origN, dim);
        System.arraycopy(ranks, 0, this.ranks, 0, origN);

        int newN = retainUniquePoints(points, reindex, this.points, ranks, origN);
        initializeObjectiveIndices(newN, dim);

        Arrays.fill(isRanked, 0, newN, false);
        Arrays.fill(checkIndicesCount, 0, newN, dim);
        Arrays.fill(indexNeededCount, 0, newN, dim);

        for (int i = 0; i < newN; ++i) {
            ArrayHelper.fillIdentity(checkIndices[i], dim);
            Arrays.fill(indexNeeded[i], 0, dim, true);
        }

        for (int d = 0; d < dim; ++d) {
            Arrays.fill(lastFrontIndex[d], 0, getMaximumPoints(), -1);
            Arrays.fill(prevFrontIndex[d], 0, getMaximumPoints(), -1);
        }

        int smallestRank = 0;

        for (int hIndex = 0, ranked = 0;
             hIndex < newN && smallestRank <= maximalMeaningfulRank && ranked < newN;
             ++hIndex) {
            for (int oIndex = 0; oIndex < dim; ++oIndex) {
                int currIndex = objectiveIndices[oIndex][hIndex];
                int[] prevFI = prevFrontIndex[oIndex];
                int[] lastFI = lastFrontIndex[oIndex];
                if (!this.isRanked[currIndex]) {
                    rankPoint(currIndex, prevFI, lastFI, smallestRank, maximalMeaningfulRank, dim);
                    ++ranked;
                }
                indexNeeded[currIndex][oIndex] = false;
                int myRank = this.ranks[currIndex];
                if (myRank <= maximalMeaningfulRank) {
                    prevFI[currIndex] = lastFI[myRank];
                    lastFI[myRank] = currIndex;
                }
                if (--indexNeededCount[currIndex] == 0) {
                    if (smallestRank < myRank + 1) {
                        smallestRank = myRank + 1;
                        if (smallestRank > maximalMeaningfulRank) {
                            break;
                        }
                    }
                }
            }
        }

        if (smallestRank > maximalMeaningfulRank) {
            for (int i = 0; i < newN; ++i) {
                if (!this.isRanked[i]) {
                    this.ranks[i] = maximalMeaningfulRank + 1;
                    this.isRanked[i] = true;
                }
            }
        }

        Arrays.fill(this.points, 0, origN, null);
        for (int i = 0; i < origN; ++i) {
            ranks[i] = this.ranks[ranks[i]];
        }
    }

    public static int retainUniquePoints(double[][] sourcePoints,
                                         int[] sortedIndices,
                                         double[][] targetPoints,
                                         int[] reindex,
                                         int n) {
        int newN = 1;
        int lastII = sortedIndices[0];
        targetPoints[0] = sourcePoints[lastII];
        reindex[lastII] = 0;
        for (int i = 1; i < n; ++i) {
            int currII = sortedIndices[i];
            if (!ArrayHelper.equal(sourcePoints[lastII], sourcePoints[currII])) {
                // Copying the point to the internal array.
                targetPoints[newN] = sourcePoints[currII];
                lastII = currII;
                ++newN;
            }
            reindex[currII] = newN - 1;
        }
        return newN;
    }

    public double[][] getTempPoints() {
        return tempPoints;
    }

    public int[] getTempRanks() {
        return tempRanks;
    }

    private boolean dominates(int i1, int i2, int M) {
        if (i1 > i2) {
            return false;
        }
        double[] p1 = points[i1];
        double[] p2 = points[i2];
        int dim = M;

        // I have not yet validated this empirically,
        // but when needed count is high, the simple loop is preferable.
        if (indexNeededCount[i1] * 3 < M) {
            int[] checkIdx = checkIndices[i1];
            boolean[] idxNeeded = indexNeeded[i1];

            int count = checkIndicesCount[i1];
            int index = 0;
            while (index < count) {
                int currIndex = checkIdx[index];
                if (idxNeeded[currIndex]) {
                    if (p1[currIndex] > p2[currIndex]) {
                        checkIndicesCount[i1] = count;
                        return false;
                    }
                    ++index;
                } else {
                    checkIdx[index] = checkIdx[--count];
                }
            }
            checkIndicesCount[i1] = count;
            return true;
        } else {
            return DominanceHelper.strictlyDominates(p1, p2, dim);
        }
    }
}
