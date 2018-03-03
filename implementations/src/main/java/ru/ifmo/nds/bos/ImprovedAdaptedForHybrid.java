package ru.ifmo.nds.bos;

import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DoubleArraySorter;

import java.util.Arrays;

public class ImprovedAdaptedForHybrid extends AbstractImproved {
    private boolean[] isRanked;
    private int[] compressedRanks;
    private int[] rankReindex;

    public ImprovedAdaptedForHybrid(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        isRanked = new boolean[maximumPoints];
        compressedRanks = new int[maximumPoints];
        rankReindex = new int[maximumPoints];
    }

    @Override
    public String getName() {
        return "Best Order Sort (improved adapted for hybrid implementation)";
    }

    @Override
    protected void closeImpl() {
        super.closeImpl();
        isRanked = null;
        compressedRanks = null;
        rankReindex = null;
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

        int newN = DoubleArraySorter.retainUniquePoints(points, reindex, this.points, ranks, N, M); // TODO delete unique ?
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
        int maxFrontIndex = Math.min(maxRank + N, getMaximumPoints());
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

    public int sortCheckedWithRespectToRanksHelperB(double[][] points, // TODO разбить на части
                                                    int[] ranks,
                                                    int goodFrom,
                                                    int goodUntil,
                                                    int weakFrom,
                                                    int weakUntil,
                                                    int M,
                                                    int maximalMeaningfulRank) {

        ArrayHelper.fillIdentity(reindex, Math.max(weakUntil, goodUntil)); // TODO rewrite for ranges
        System.arraycopy(ranks, 0, this.ranks, 0, Math.max(weakUntil, goodUntil)); // TODO rewrite for ranges
        Arrays.fill(compressedRanks, -1); // TODO delete init
        Arrays.fill(rankReindex, 0, Math.max(weakUntil, goodUntil), -1); // TODO delete init

        int newWeakUntil = weakFrom + DoubleArraySorter.retainUniquePoints(points, reindex, this.points, rankReindex, weakFrom, weakFrom, weakUntil, M);
        int newGoodUntil = goodFrom + DoubleArraySorter.retainUniquePoints(points, reindex, this.points, rankReindex, goodFrom, goodFrom, goodUntil, M);

        for (int i = weakFrom; i < weakUntil; i++) {
            if (compressedRanks[rankReindex[i]] < this.ranks[i]) { // TODO max
                compressedRanks[rankReindex[i]] = this.ranks[i];
            }
        }
        for (int i = goodFrom; i < goodUntil; i++) {
            if (compressedRanks[rankReindex[i]] < this.ranks[i]) { // TODO max
                compressedRanks[rankReindex[i]] = this.ranks[i];
            }
        }

        for (int i = 0; i < getMaximumDimension(); i++) { // TODO delete init
            Arrays.fill(objectiveIndices[i], -1);
        }

        initializeObjectiveIndices(weakFrom, newWeakUntil, goodFrom, newGoodUntil, M);

        Arrays.fill(isRanked, false); // TODO delete

        Arrays.fill(isRanked, weakFrom, newWeakUntil, false);
        Arrays.fill(isRanked, goodFrom, newGoodUntil, true);
        Arrays.fill(checkIndicesCount, weakFrom, newWeakUntil, M);
        Arrays.fill(checkIndicesCount, goodFrom, newGoodUntil, M);
        Arrays.fill(indexNeededCount, weakFrom, newWeakUntil, M);
        Arrays.fill(indexNeededCount, goodFrom, newGoodUntil, M);

        for (int i = weakFrom; i < newWeakUntil; ++i) {
            ArrayHelper.fillIdentity(checkIndices[i], M);
            Arrays.fill(indexNeeded[i], 0, M, true);
        }

        for (int i = goodFrom; i < newGoodUntil; ++i) {
            ArrayHelper.fillIdentity(checkIndices[i], M);
            Arrays.fill(indexNeeded[i], 0, M, true);
        }

        int maxRank = ArrayHelper.max(this.compressedRanks, weakFrom, newWeakUntil);
        maxRank = Math.max(maxRank, ArrayHelper.max(this.compressedRanks, goodFrom, newGoodUntil)); // TODO можно перенести в циклы выше
        int sizeUnion = (newWeakUntil - weakFrom) + (newGoodUntil - goodFrom);
        maximalMeaningfulRank = Math.min(maximalMeaningfulRank, maxRank + sizeUnion);
        int maxFrontIndex = Math.min(maxRank + sizeUnion, getMaximumPoints());
        for (int d = 0; d < M; ++d) {
            Arrays.fill(lastFrontIndex[d], 0, maxFrontIndex, -1);
            Arrays.fill(prevFrontIndex[d], 0, maxFrontIndex, -1);
        }

        int smallestRank = 0;

        for (int hIndex = 0, ranked = (newGoodUntil - goodFrom);
             hIndex < sizeUnion && smallestRank <= maximalMeaningfulRank && ranked < sizeUnion;
             ++hIndex) {
            for (int oIndex = 0; oIndex < M && ranked < sizeUnion; ++oIndex) {
                int currIndex = objectiveIndices[oIndex][hIndex];
                int[] prevFI = prevFrontIndex[oIndex];
                int[] lastFI = lastFrontIndex[oIndex];
                if (!this.isRanked[currIndex]) {
                    rankPointHelperB(currIndex, prevFI, lastFI, smallestRank, maximalMeaningfulRank, M);
                    ++ranked;
                }
                if (goodFrom <= currIndex && currIndex < newGoodUntil) {
                    indexNeeded[currIndex][oIndex] = false;
                    int myRank = this.compressedRanks[currIndex];
                    if (myRank <= maximalMeaningfulRank) {
                        prevFI[currIndex] = lastFI[myRank];
                        lastFI[myRank] = currIndex;
                    }
                    if (--indexNeededCount[currIndex] == 0) { // TODO где-то тут скорее всего ошибка, см sequentialSearchRank
                        if (smallestRank < myRank + 1) {
                            smallestRank = myRank + 1;
                            if (smallestRank > maximalMeaningfulRank) {
                                break;
                            }
                        }
                    }
                }
            }
        }

        if (smallestRank > maximalMeaningfulRank) {
            for (int i = weakFrom; i < newWeakUntil; ++i) {
                if (!this.isRanked[i]) {
                    this.compressedRanks[i] = maximalMeaningfulRank + 1;
                    this.isRanked[i] = true;
                }
            }
        }

        Arrays.fill(this.points, 0, Math.max(weakUntil, goodUntil), null);
        for (int i = weakFrom; i < weakUntil; i++) {
            ranks[i] = this.compressedRanks[rankReindex[i]];
        }

        int resultWeakUntil = weakUntil;
        for (int i = weakUntil - 1; i >= weakFrom; --i) {
            if (ranks[i] > maximalMeaningfulRank) {
                resultWeakUntil = i;
            } else {
                break;
            }
        }
        return resultWeakUntil;
    }

    private void rankPointHelperB(int currIndex, int[] prevFI, int[] lastFI, int smallestRank, int maximalMeaningfulRank, int M) {
        int currRank = Math.max(smallestRank, compressedRanks[currIndex]);

        int resultRank = sequentialSearchRank(currRank, currIndex, prevFI, lastFI, maximalMeaningfulRank, M);

        this.compressedRanks[currIndex] = Math.max(this.compressedRanks[currIndex], resultRank);
        this.isRanked[currIndex] = true;
    }

    private void initializeObjectiveIndices(int weakFrom, int weakUntil, int goodFrom, int goodUntil, int M) {
        int newN = (weakUntil - weakFrom) + (goodUntil - goodFrom);
        int[] resolver = new int[Math.max(weakUntil, goodUntil)]; // TODO fix max // TODO delete?
        Arrays.fill(resolver, -1);
        for (int d = 0; d < M; ++d) {
            int[] currentObjectiveIndex = objectiveIndices[d]; // TODO do not it use alloc ?
            for (int i = goodFrom; i < goodUntil; i++) {
                currentObjectiveIndex[i - goodFrom] = i;
            }
            for (int i = weakFrom; i < weakUntil; i++) {
                currentObjectiveIndex[goodUntil - goodFrom + i - weakFrom] = i;
            }
            if (d > 0) {
                sorter.sortWhileResolvingEqual(this.points, currentObjectiveIndex, 0, newN, d, resolver);
            } else {
                for (int i = goodFrom; i < goodUntil; ++i) {
                    resolver[i] = i;
                }
                for (int i = weakFrom; i < weakUntil; ++i) {
                    resolver[i] = i;
                }

                sorter.sortWhileResolvingEqual(this.points, currentObjectiveIndex, 0, newN, 0, resolver); // TODO подумать, можно ли избежать
            }
        }
    }
}
