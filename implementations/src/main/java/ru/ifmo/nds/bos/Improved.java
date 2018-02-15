package ru.ifmo.nds.bos;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;
import ru.ifmo.nds.util.DoubleArraySorter;

import java.util.Arrays;

public class Improved extends NonDominatedSorting {
    private int[][] objectiveIndices;
    private int[] reindex;
    private double[][] points;
    private int[] ranks;
    private boolean[] isRanked;
    private int[][] lastFrontIndex;
    private int[][] prevFrontIndex;

    private int[][] checkIndices;
    private int[] checkIndicesCount;
    private boolean[][] indexNeeded;
    private int[] indexNeededCount;

    private DoubleArraySorter sorter;

    public Improved(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        objectiveIndices = new int[maximumDimension][maximumPoints];
        lastFrontIndex = new int[maximumDimension][maximumPoints];
        prevFrontIndex = new int[maximumDimension][maximumPoints];
        checkIndices = new int[maximumPoints][maximumDimension];
        checkIndicesCount = new int[maximumPoints];
        indexNeededCount = new int[maximumPoints];
        indexNeeded = new boolean[maximumPoints][maximumDimension];
        reindex = new int[maximumPoints];
        points = new double[maximumPoints][];
        ranks = new int[maximumPoints];
        isRanked = new boolean[maximumPoints];
        sorter = new DoubleArraySorter(maximumPoints);
    }

    @Override
    public String getName() {
        return "Best Order Sort (improved implementation)";
    }

    @Override
    protected void closeImpl() {
        objectiveIndices = null;
        lastFrontIndex = null;
        prevFrontIndex = null;
        checkIndices = null;
        checkIndicesCount = null;
        indexNeeded = null;
        indexNeededCount = null;
        reindex = null;
        points = null;
        ranks = null;
        sorter = null;
    }

    private boolean dominates(int i1, int i2) {
        if (i1 > i2) {
            return false;
        }
        double[] p1 = points[i1];
        double[] p2 = points[i2];
        int dim = p1.length;

        // I have not yet validated this empirically,
        // but when needed count is high, the simple loop is preferable.
        if (indexNeededCount[i1] * 3 < p1.length) {
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

    private void initializeObjectiveIndices(int newN, int dim) {
        for (int d = 0; d < dim; ++d) {
            int[] currentObjectiveIndex = objectiveIndices[d];
            ArrayHelper.fillIdentity(currentObjectiveIndex, newN);
            if (d > 0) {
                sorter.sortWhileResolvingEqual(this.points, currentObjectiveIndex, 0, newN, d, objectiveIndices[0]);
            }
        }
    }

    private void rankPoint(int currIndex, int[] prevFI, int[] lastFI, int smallestRank, int maximalMeaningfulRank) {
        int currRank = Math.max(smallestRank, ranks[currIndex]);

        // This is currently implemented as sequential search.
        // A binary search implementation is expected as well.
        while (currRank <= maximalMeaningfulRank) {
            int prevIndex = lastFI[currRank];
            boolean someoneDominatesMe = false;
            while (prevIndex != -1) {
                if (dominates(prevIndex, currIndex)) {
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
        Arrays.fill(ranks, 0, ranks.length, 0);
        sortCheckedWithRespectToRanks(points, ranks, maximalMeaningfulRank);
    }

    @Override
    protected void sortCheckedWithRespectToRanks(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int origN = ranks.length;
        int dim = points[0].length;
        ArrayHelper.fillIdentity(reindex, origN);
        sorter.lexicographicalSort(points, reindex, 0, origN, dim);
        System.arraycopy(ranks, 0, this.ranks, 0, ranks.length);

        int newN = DoubleArraySorter.retainUniquePoints(points, reindex, this.points, ranks);
        initializeObjectiveIndices(newN, dim);
        maximalMeaningfulRank = Math.min(maximalMeaningfulRank, newN - 1);

        Arrays.fill(isRanked, 0, newN, false);
        Arrays.fill(checkIndicesCount, 0, newN, dim);
        Arrays.fill(indexNeededCount, 0, newN, dim);

        for (int i = 0; i < newN; ++i) {
            ArrayHelper.fillIdentity(checkIndices[i], dim);
            Arrays.fill(indexNeeded[i], 0, dim, true);
        }

        for (int d = 0; d < dim; ++d) {
            Arrays.fill(lastFrontIndex[d], 0, newN, -1);
            Arrays.fill(prevFrontIndex[d], 0, newN, -1);
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
                    rankPoint(currIndex, prevFI, lastFI, smallestRank, maximalMeaningfulRank);
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
}
