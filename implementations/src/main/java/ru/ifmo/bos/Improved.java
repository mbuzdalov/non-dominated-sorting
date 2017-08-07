package ru.ifmo.bos;

import ru.ifmo.NonDominatedSorting;
import ru.ifmo.util.DoubleArraySorter;

import java.util.Arrays;

public class Improved extends NonDominatedSorting {
    private int[][] objectiveIndices;
    private int[] reindex;
    private double[][] points;
    private int[] ranks;
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
        sorter = new DoubleArraySorter(maximumPoints);
    }

    @Override
    public String getName() {
        return "Best Order Sort (improved implementation)";
    }

    @Override
    protected void closeImpl() throws Exception {
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
            for (int i = 0; i < p1.length; ++i) {
                if (p1[i] > p2[i]) {
                    return false;
                }
            }
            return true;
        }
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int origN = ranks.length;
        int dim = points[0].length;
        for (int i = 0; i < origN; ++i) {
            reindex[i] = i;
        }
        sorter.lexicographicalSort(points, reindex, 0, origN, dim);
        int newN = DoubleArraySorter.retainUniquePoints(points, reindex, this.points, ranks);
        for (int d = 0; d < dim; ++d) {
            int[] currentObjectiveIndex = objectiveIndices[d];
            for (int i = 0; i < newN; ++i) {
                currentObjectiveIndex[i] = i;
            }
            if (d > 0) {
                sorter.sortWhileResolvingEqual(this.points, currentObjectiveIndex, 0, newN, d, objectiveIndices[0]);
            }
        }
        maximalMeaningfulRank = Math.min(maximalMeaningfulRank, newN - 1);
        for (int i = 0; i < newN; ++i) {
            this.ranks[i] = -1;
            checkIndicesCount[i] = dim;
            indexNeededCount[i] = dim;
            for (int d = 0; d < dim; ++d) {
                checkIndices[i][d] = d;
                indexNeeded[i][d] = true;
            }
        }
        for (int d = 0; d < dim; ++d) {
            Arrays.fill(lastFrontIndex[d], 0, newN, -1);
            Arrays.fill(prevFrontIndex[d], 0, newN, -1);
        }

        int smallestRank = 0;

        for (int hIndex = 0, ranked = 0; hIndex < newN && smallestRank <= maximalMeaningfulRank && ranked < newN; ++hIndex) {
            for (int oIndex = 0; oIndex < dim; ++oIndex) {
                int currIndex = objectiveIndices[oIndex][hIndex];
                int[] prevFI = prevFrontIndex[oIndex];
                int[] lastFI = lastFrontIndex[oIndex];
                if (this.ranks[currIndex] == -1) {
                    int currRank = smallestRank;
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
                    this.ranks[currIndex] = currRank;
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
                if (this.ranks[i] == -1) {
                    this.ranks[i] = maximalMeaningfulRank + 1;
                }
            }
        }

        for (int i = 0; i < origN; ++i) {
            ranks[i] = this.ranks[ranks[i]];
            this.points[i] = null;
        }
    }
}
