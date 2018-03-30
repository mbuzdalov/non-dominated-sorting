package ru.ifmo.nds.bos;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;
import ru.ifmo.nds.util.DoubleArraySorter;

abstract class AbstractImproved extends NonDominatedSorting {
    int[][] objectiveIndices;
    int[] reindex;
    double[][] points;
    int[] ranks;
    int[][] lastFrontIndex;
    int[][] prevFrontIndex;

    int[][] checkIndices;
    int[] checkIndicesCount;
    boolean[][] indexNeeded;
    int[] indexNeededCount;

    DoubleArraySorter sorter;

    AbstractImproved(int maximumPoints, int maximumDimension) {
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
        sorter = new DoubleArraySorter(maximumPoints);
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
        return dominates(i1, i2, points[i1].length);
    }

    boolean dominates(int i1, int i2, int M) {
        if (i1 > i2) {
            return false;
        }
        double[] p1 = points[i1];
        double[] p2 = points[i2];

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
            return DominanceHelper.strictlyDominates(p1, p2, M);
        }
    }

    void initializeObjectiveIndices(int N, int M) {
        for (int d = 0; d < M; ++d) {
            int[] currentObjectiveIndex = objectiveIndices[d];
            ArrayHelper.fillIdentity(currentObjectiveIndex, N);
            if (d > 0) {
                sorter.sortWhileResolvingEqual(this.points, currentObjectiveIndex, 0, N, d, objectiveIndices[0]);
            }
        }
    }

    int sequentialSearchRank(int currRank,
                             int currIndex,
                             int[] prevFI,
                             int[] lastFI,
                             int maximalMeaningfulRank) {
        return sequentialSearchRank(currRank, currIndex, prevFI, lastFI, maximalMeaningfulRank, points[0].length);
    }

    int sequentialSearchRank(int currRank,
                             int currIndex,
                             int[] prevFI,
                             int[] lastFI,
                             int maximalMeaningfulRank,
                             int M) {
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
        return currRank;
    }
}
