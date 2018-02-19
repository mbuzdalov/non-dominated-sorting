package ru.ifmo.nds.bos;

import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DoubleArraySorter;

import java.util.Arrays;

public class ImprovedAdaptedForHybrid extends AbstractImproved {
    private boolean[] isRanked;

    public ImprovedAdaptedForHybrid(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        isRanked = new boolean[maximumPoints];
    }

    @Override
    public String getName() {
        return "Best Order Sort (improved adapter for hybrid implementation)";
    }

    @Override
    protected void closeImpl() {
        super.closeImpl();
        isRanked = null;
    }

    private void rankPoint(int currIndex, int[] prevFI, int[] lastFI, int smallestRank, int maximalMeaningfulRank, int M) {
        int currRank = Math.max(smallestRank, ranks[currIndex]);

        int resultRank = sequentialSearchRank(currRank, currIndex, prevFI, lastFI, maximalMeaningfulRank, M);

        this.ranks[currIndex] = Math.max(this.ranks[currIndex], resultRank);
        this.isRanked[currIndex] = true;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        throw new UnsupportedOperationException("ImprovedAdaptedForHybrid sorting doesn't work alone");
    }

    public void sortCheckedWithRespectToRanks(double[][] points, int[] ranks, int N, int M, int maximalMeaningfulRank) {
        ArrayHelper.fillIdentity(reindex, N);
        System.arraycopy(ranks, 0, this.ranks, 0, N);

        int newN = DoubleArraySorter.retainUniquePoints(points, reindex, this.points, ranks, N, M);
        initializeObjectiveIndices(newN, M);

        Arrays.fill(isRanked, 0, newN, false);
        Arrays.fill(checkIndicesCount, 0, newN, M);
        Arrays.fill(indexNeededCount, 0, newN, M);

        for (int i = 0; i < newN; ++i) {
            ArrayHelper.fillIdentity(checkIndices[i], M);
            Arrays.fill(indexNeeded[i], 0, M, true);
        }

        int maxRank = ArrayHelper.max(this.ranks, 0, N);
        maximalMeaningfulRank = Math.min(maximalMeaningfulRank, maxRank + N);
        int maxFrontIndex = Math.min(maxRank + N + 1, getMaximumPoints());
        for (int d = 0; d < M; ++d) {
            Arrays.fill(lastFrontIndex[d], 0, maxFrontIndex, -1);
            Arrays.fill(prevFrontIndex[d], 0, maxFrontIndex, -1);
        }

        int smallestRank = 0;

        for (int hIndex = 0, ranked = 0;
             hIndex < newN && smallestRank <= maximalMeaningfulRank && ranked < newN;
             ++hIndex) {
            for (int oIndex = 0; oIndex < M; ++oIndex) {
                int currIndex = objectiveIndices[oIndex][hIndex];
                int[] prevFI = prevFrontIndex[oIndex];
                int[] lastFI = lastFrontIndex[oIndex];
                if (!this.isRanked[currIndex]) {
                    rankPoint(currIndex, prevFI, lastFI, smallestRank, maximalMeaningfulRank, M);
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

        Arrays.fill(this.points, 0, N, null);
        for (int i = 0; i < N; ++i) {
            ranks[i] = this.ranks[ranks[i]];
        }
    }
}
