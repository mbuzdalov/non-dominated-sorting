package ru.ifmo.nds.bos;

import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DoubleArraySorter;

import java.util.Arrays;

public class ImprovedAdaptedForHybrid extends AbstractImproved {
    private boolean[] isRanked;
    private int[] compressedRanks;
    private int[] rankReindex;
    private int[] possibleRanks;
    private int sizePossibleRanks;
    private int[] minPossibleRankIndices;
    private int[] maxPossibleRankIndices;


    public ImprovedAdaptedForHybrid(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        isRanked = new boolean[maximumPoints];
        compressedRanks = new int[maximumPoints];
        rankReindex = new int[maximumPoints];
        possibleRanks = new int[maximumPoints];
        minPossibleRankIndices = new int[maximumDimension];
        maxPossibleRankIndices = new int[maximumDimension];
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
        possibleRanks = null;
        minPossibleRankIndices = null;
        maxPossibleRankIndices = null;
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
        System.arraycopy(ranks, 0, this.ranks, 0, N);

        // Instead of retainUniquePoints
        ArrayHelper.fillIdentity(ranks, N);
        System.arraycopy(points, 0, this.points, 0, N);

        initializeObjectiveIndices(N, M);

        Arrays.fill(isRanked, 0, N, false);
        Arrays.fill(checkIndicesCount, 0, N, M);
        Arrays.fill(indexNeededCount, 0, N, M);

        for (int i = 0; i < N; ++i) {
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

//        int smallestRank = ArrayHelper.min(this.ranks, 0, N); // примерно так же как smallestRank = 0
        int smallestRank = 0;

        for (int hIndex = 0, ranked = 0;
             hIndex < N && smallestRank <= maximalMeaningfulRank && ranked < N;
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
            for (int i = 0; i < N; ++i) {
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

    public void sortCheckedWithRespectToRanksHelperB(double[][] points,
                                                     int[] ranks,
                                                     int goodFrom,
                                                     int goodUntil,
                                                     int weakFrom,
                                                     int weakUntil,
                                                     int M,
                                                     int maximalMeaningfulRank) {

        System.arraycopy(ranks, weakFrom, this.ranks, weakFrom, weakUntil - weakFrom);
        System.arraycopy(ranks, goodFrom, this.ranks, goodFrom, goodUntil - goodFrom);

        for (int i = weakFrom; i < weakUntil; ++i) {
            reindex[i] = i;
            points[i][M] = ranks[i]; // для сжатия
        }
        for (int i = goodFrom; i < goodUntil; ++i) {
            reindex[i] = i;
        }

        // Для сжатия точек из weak добавим еще один критерий - текущий ранг.
        // Всегда M < maximumDimension
        int newWeakUntil = weakFrom + DoubleArraySorter.retainUniquePoints(points, reindex, this.points, rankReindex, weakFrom, weakFrom, weakUntil, M + 1);

        int newGoodUntil = goodFrom + DoubleArraySorter.retainUniquePoints(points, reindex, this.points, rankReindex, goodFrom, goodFrom, goodUntil, M);

        initializeCompressedRanks(weakFrom, weakUntil, goodFrom, goodUntil);
        initializePossibilityRanks(goodFrom, newGoodUntil, M);

        // Note: this function uses checkIndicesCount like internal temporary buffer,
        // please place initialization of checkIndicesCount bellow
        initializeObjectiveIndices(weakFrom, newWeakUntil, goodFrom, newGoodUntil, M);

        int maxRank = -1;
        for (int i = weakFrom; i < newWeakUntil; ++i) {
            isRanked[i] = false;
            checkIndicesCount[i] = M;
            indexNeededCount[i] = M;
            ArrayHelper.fillIdentity(checkIndices[i], M);
            Arrays.fill(indexNeeded[i], 0, M, true);
            maxRank = Math.max(maxRank, this.compressedRanks[i]);
        }

        for (int i = goodFrom; i < newGoodUntil; ++i) {
            isRanked[i] = true;
            checkIndicesCount[i] = M;
            indexNeededCount[i] = M;
            ArrayHelper.fillIdentity(checkIndices[i], M);
            Arrays.fill(indexNeeded[i], 0, M, true);
            maxRank = Math.max(maxRank, this.compressedRanks[i]);
        }

        int sizeUnion = newWeakUntil - weakFrom + newGoodUntil - goodFrom;
        maximalMeaningfulRank = Math.min(maximalMeaningfulRank, maxRank + sizeUnion);
        int maxFrontIndex = Math.min(getMaximumPoints(), maxRank + sizeUnion);
        for (int d = 0; d < M; ++d) {
            Arrays.fill(lastFrontIndex[d], 0, maxFrontIndex, -1);
            Arrays.fill(prevFrontIndex[d], 0, maxFrontIndex, -1);
        }

//        int smallestRank = ArrayHelper.min(this.compressedRanks, goodFrom, newGoodUntil); // хуже
        int smallestRank = 0;

        for (int hIndex = 0, ranked = (newGoodUntil - goodFrom);
             hIndex < sizeUnion && smallestRank <= maximalMeaningfulRank && ranked < sizeUnion;
             ++hIndex) {
            for (int oIndex = 0; oIndex < M && ranked < sizeUnion && smallestRank <= maximalMeaningfulRank; ++oIndex) {
                int minPossibleRankId = minPossibleRankIndices[oIndex];
                int maxPossibleRankId = maxPossibleRankIndices[oIndex];
                int currIndex = objectiveIndices[oIndex][hIndex];
                int[] prevFI = prevFrontIndex[oIndex];
                int[] lastFI = lastFrontIndex[oIndex];
                if (!this.isRanked[currIndex]) {
                    rankPointHelperB(currIndex,
                            prevFI,
                            lastFI,
                            minPossibleRankId,
                            maxPossibleRankId,
                            maximalMeaningfulRank,
                            M);
                    ++ranked;
                }
                if (goodFrom <= currIndex && currIndex < newGoodUntil) {
                    updateMinMaxPossibleRankIndices(oIndex, compressedRanks[currIndex]);
                    indexNeeded[currIndex][oIndex] = false;
                    int myRank = this.compressedRanks[currIndex];
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
        }

        if (smallestRank > maximalMeaningfulRank) {
            for (int i = weakFrom; i < newWeakUntil; ++i) {
                if (!this.isRanked[i]) {
                    this.compressedRanks[i] = maximalMeaningfulRank + 1;
                    this.isRanked[i] = true;
                }
            }
        }

        Arrays.fill(this.points, weakFrom, weakFrom, null);
        Arrays.fill(this.points, goodFrom, goodFrom, null);
        for (int i = weakFrom; i < weakUntil; ++i) {
            ranks[i] = this.compressedRanks[rankReindex[i]];
        }
    }

    private void initializeCompressedRanks(int weakFrom, int weakUntil, int goodFrom, int goodUntil) {
        Arrays.fill(compressedRanks, goodFrom, goodUntil, -1);
        for (int i = weakFrom; i < weakUntil; ++i) {
            compressedRanks[rankReindex[i]] = this.ranks[i];
        }
        for (int i = goodFrom; i < goodUntil; ++i) {
            compressedRanks[rankReindex[i]] = Math.max(compressedRanks[rankReindex[i]], this.ranks[i]);
        }
    }

    private void updateMinMaxPossibleRankIndices(int obj, int newRank) {
        if (maxPossibleRankIndices[obj] == minPossibleRankIndices[obj]) { // we met the first point from good
            int id = Arrays.binarySearch(possibleRanks, 0, sizePossibleRanks, newRank);
            minPossibleRankIndices[obj] = id;
            maxPossibleRankIndices[obj] = id + 1;
            return;
        }

        if (newRank <= possibleRanks[maxPossibleRankIndices[obj] - 1]
                && newRank >= possibleRanks[minPossibleRankIndices[obj]]) {
            return; // current borders is correct
        }

        while (newRank < possibleRanks[minPossibleRankIndices[obj]]) {
            --minPossibleRankIndices[obj];
        }

        while (newRank > possibleRanks[maxPossibleRankIndices[obj] - 1]) {
            ++maxPossibleRankIndices[obj];
        }
    }

    private void initializePossibilityRanks(int from, int until, int M) {
        System.arraycopy(compressedRanks, from, possibleRanks, 0, until - from);

        Arrays.sort(possibleRanks, 0, until - from);

        sizePossibleRanks = 1;
        for (int i = 1; i < until - from; ++i) {
            if (possibleRanks[i] != possibleRanks[i - 1]) {
                possibleRanks[sizePossibleRanks] = possibleRanks[i];
                ++sizePossibleRanks;
            }
        }

        Arrays.fill(minPossibleRankIndices, 0, M, 0);
        Arrays.fill(maxPossibleRankIndices, 0, M, 0);
    }

    private void rankPointHelperB(int currIndex,
                                  int[] prevFI,
                                  int[] lastFI,
                                  int minPossibleRankId,
                                  int maxPossibleRankId,
                                  int maximalMeaningfulRank,
                                  int M) {
        int smallestRank = compressedRanks[currIndex];
        int resultRank;
        if (minPossibleRankId == maxPossibleRankId) { // we haven't met any point from good yet
            resultRank = sequentialSearchRank(smallestRank, currIndex, prevFI, lastFI, maximalMeaningfulRank, M);
        } else {
            resultRank = sequentialSearchRankHelperB(
                    minPossibleRankId,
                    maxPossibleRankId,
                    currIndex,
                    prevFI,
                    lastFI,
                    M);
        }

        this.compressedRanks[currIndex] = Math.max(this.compressedRanks[currIndex], resultRank);
        this.isRanked[currIndex] = true;
    }

    private int sequentialSearchRankHelperB(int minPossibleRankId,
                                            int maxPossibleRankId,
                                            int currIndex,
                                            int[] prevFI,
                                            int[] lastFI,
                                            int M) {
        // A binary search implementation isn't possible here.

        int currRankIndex = maxPossibleRankId - 1;

        while (currRankIndex >= minPossibleRankId) {
            int prevIndex = lastFI[possibleRanks[currRankIndex]];
            boolean someoneDominatesMe = false;
            while (prevIndex != -1) {
                if (dominates(prevIndex, currIndex, M)) {
                    someoneDominatesMe = true;
                    break;
                }
                if (isEquals(prevIndex, currIndex, M)) {
                    someoneDominatesMe = true; // this is valid only for helperB
                    break;
                } else {
                    prevIndex = prevFI[prevIndex];
                }
            }
            if (someoneDominatesMe) {
                break;
            }

            --currRankIndex;
        }

        if (currRankIndex < minPossibleRankId) {
            return 0;
        }
        return possibleRanks[currRankIndex] + 1;
    }

    private boolean isEquals(int i1, int i2, int M) {
        double[] p1 = points[i1];
        double[] p2 = points[i2];

        for (int i = 0; i < M; ++i) {
            if (p1[i] != p2[i]) {
                return false;
            }
        }
        return true;
    }

    private void initializeObjectiveIndices(int weakFrom, int weakUntil, int goodFrom, int goodUntil, int M) {
        // Variable checkIndicesCount is used like internal temporary buffer
        int newN = weakUntil - weakFrom + goodUntil - goodFrom;
        for (int i = goodFrom; i < goodUntil; ++i) {
            checkIndicesCount[i] = i; // checkIndicesCount == resolver
        }
        for (int i = weakFrom; i < weakUntil; ++i) {
            checkIndicesCount[i] = i; // checkIndicesCount == resolver
        }
        for (int d = 0; d < M; ++d) {
            int[] currentObjectiveIndex = objectiveIndices[d];
            for (int i = goodFrom; i < goodUntil; ++i) {
                currentObjectiveIndex[i - goodFrom] = i;
            }
            for (int i = weakFrom; i < weakUntil; ++i) {
                currentObjectiveIndex[goodUntil - goodFrom + i - weakFrom] = i;
            }
            sorter.sortWhileResolvingEqual(this.points, currentObjectiveIndex, 0, newN, d, checkIndicesCount); // checkIndicesCount == resolver
        }
    }
}
