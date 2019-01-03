package ru.ifmo.nds.bos;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DominanceHelper;
import ru.ifmo.nds.util.ArraySorter;

import java.util.Arrays;

public class Improved extends NonDominatedSorting {
    private int[][] objectiveIndices;
    private double[][] points;
    private int[] ranks;
    private int[][] lastFrontIndex;
    private int[][] prevFrontIndex;

    private int[] indexNeededCount;

    public Improved(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        objectiveIndices = new int[maximumDimension][maximumPoints];
        lastFrontIndex = new int[maximumDimension][maximumPoints];
        prevFrontIndex = new int[maximumDimension][maximumPoints];
        indexNeededCount = new int[maximumPoints];
        points = new double[maximumPoints][];
        ranks = new int[maximumPoints];
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
        indexNeededCount = null;
        points = null;
        ranks = null;
    }

    private void initializeObjectiveIndices(int newN, int dim) {
        for (int d = 0; d < dim; ++d) {
            int[] currentObjectiveIndex = objectiveIndices[d];
            ArrayHelper.fillIdentity(currentObjectiveIndex, newN);
            if (d > 0) {
                sorter.sortComparingByIndicesIfEqual(this.points, currentObjectiveIndex, 0, newN, d);
            }
        }
    }

    private void rankPoint(int currIndex, int[] prevFI, int[] lastFI, int smallestRank, int maximalMeaningfulRank) {
        double[] p2 = points[currIndex];
        int maxObj = p2.length - 1;
        int currRank = smallestRank;
        // This is currently implemented as sequential search.
        // A binary search implementation is expected as well.
        while (currRank <= maximalMeaningfulRank) {
            int prevIndex = lastFI[currRank];
            boolean someoneDominatesMe = false;
            while (prevIndex != -1) {
                if (prevIndex < currIndex && // For now, we totally ignore that some coordinates are unneeded.
                        DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(points[prevIndex], p2, maxObj)) {
                    someoneDominatesMe = true;
                    break;
                }
                prevIndex = prevFI[prevIndex];
            }
            if (!someoneDominatesMe) {
                break;
            }
            ++currRank;
        }
        this.ranks[currIndex] = currRank;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int origN = ranks.length;
        int dim = points[0].length;
        ArrayHelper.fillIdentity(indices, origN);
        sorter.lexicographicalSort(points, indices, 0, origN, dim);
        int newN = ArraySorter.retainUniquePoints(points, indices, this.points, ranks);
        initializeObjectiveIndices(newN, dim);
        maximalMeaningfulRank = Math.min(maximalMeaningfulRank, newN - 1);

        Arrays.fill(this.ranks, 0, newN, -1);
        Arrays.fill(indexNeededCount, 0, newN, dim);

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
                if (this.ranks[currIndex] == -1) {
                    rankPoint(currIndex, prevFI, lastFI, smallestRank, maximalMeaningfulRank);
                    ++ranked;
                }
                int myRank = this.ranks[currIndex];
                if (myRank <= maximalMeaningfulRank) {
                    prevFI[currIndex] = lastFI[myRank];
                    lastFI[myRank] = currIndex;
                }
                if (--indexNeededCount[currIndex] == 0) {
                    if (smallestRank <= myRank) {
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

        Arrays.fill(this.points, 0, origN, null);
        for (int i = 0; i < origN; ++i) {
            ranks[i] = this.ranks[ranks[i]];
        }
    }
}
