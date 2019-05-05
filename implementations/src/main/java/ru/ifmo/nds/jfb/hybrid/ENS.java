package ru.ifmo.nds.jfb.hybrid;

import ru.ifmo.nds.jfb.HybridAlgorithmWrapper;
import ru.ifmo.nds.jfb.JFBBase;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.ArraySorter;
import ru.ifmo.nds.util.DominanceHelper;

import java.util.Arrays;

public final class ENS extends HybridAlgorithmWrapper {
    private final int threshold3D;
    private final int thresholdAll;

    public ENS(int threshold3D, int thresholdAll) {
        this.threshold3D = threshold3D;
        this.thresholdAll = thresholdAll;
    }

    @Override
    public boolean supportsMultipleThreads() {
        return true;
    }

    @Override
    public String getName() {
        return "ENS (threshold 3D = " + threshold3D + ", threshold all = " + thresholdAll + ")";
    }

    @Override
    public HybridAlgorithmWrapper.Instance create(int[] ranks, int[] indices, double[][] points, double[][] transposedPoints) {
        return new Instance(ranks, indices, points, threshold3D, thresholdAll);
    }

    private static final class Instance extends HybridAlgorithmWrapper.Instance {
        private static final int STORAGE_MULTIPLE = 5;

        private final int[] space;
        private final int[] ranks;
        private final int[] indices;
        private final double[][] points;
        private final double[][] exPoints;

        private final int threshold3D;
        private final int thresholdAll;

        private Instance(int[] ranks, int[] indices, double[][] points, int threshold3D, int thresholdAll) {
            this.ranks = ranks;
            this.indices = indices;
            this.points = points;
            this.exPoints = new double[points.length][];
            this.space = new int[STORAGE_MULTIPLE * indices.length];
            this.threshold3D = threshold3D;
            this.thresholdAll = thresholdAll;
        }

        private boolean notHookCondition(int size, int obj) {
            switch (obj) {
                case 1:
                    return true;
                case 2:
                    return size >= threshold3D;
                default:
                    return size >= thresholdAll;
            }
        }

        private boolean checkIfDominatesA(int sliceIndex, int obj, int weakIndex) {
            int sliceRank = space[sliceIndex];
            if (ranks[weakIndex] > sliceRank) {
                return true;
            }
            int virtualGoodIndex = space[sliceIndex + 2];
            double[] wp = points[weakIndex];
            while (virtualGoodIndex != -1) {
                int realGoodIndex = space[virtualGoodIndex];
                if (DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(points[realGoodIndex], wp, obj)) {
                    ranks[weakIndex] = 1 + sliceRank;
                    return true;
                }
                virtualGoodIndex = space[virtualGoodIndex + 1];
            }
            return false;
        }

        private void initNewSliceA(int prevSlice, int currSlice, int nextSlice, int rank, int firstPointIndex) {
            space[currSlice] = rank;
            space[currSlice + 1] = nextSlice;
            space[currSlice + 2] = firstPointIndex;
            if (prevSlice != -1) {
                space[prevSlice + 1] = currSlice;
            }
        }

        @Override
        public int helperAHook(int from, int until, int obj, int maximalMeaningfulRank) {
            if (notHookCondition(until - from, obj)) {
                return -1;
            }

            int sliceOffset = from * STORAGE_MULTIPLE;
            int pointOffset = sliceOffset + 3 * (until - from);

            int sliceCurrent = sliceOffset - 3;
            int sliceFirst = -1;

            int minOverflow = until;
            for (int i = from, pointIndex = pointOffset; i < until; ++i) {
                int ii = indices[i];
                if (sliceFirst == -1 || checkIfDominatesA(sliceFirst, obj, ii)) {
                    if (ranks[ii] <= maximalMeaningfulRank) {
                        sliceCurrent += 3;
                        initNewSliceA(-1, sliceCurrent, sliceFirst, ranks[ii], pointIndex);
                        space[pointIndex] = ii;
                        space[pointIndex + 1] = -1;
                        sliceFirst = sliceCurrent;
                        pointIndex += 2;
                    } else if (minOverflow > i) {
                        minOverflow = i;
                    }
                } else {
                    int prevSlice = sliceFirst, nextSlice;
                    while ((nextSlice = space[prevSlice + 1]) != -1) {
                        if (checkIfDominatesA(nextSlice, obj, ii)) {
                            break;
                        }
                        prevSlice = nextSlice;
                    }
                    // prevSlice does not dominate, nextSlice already dominates
                    space[pointIndex] = ii;
                    int currRank = ranks[ii];
                    if (currRank == space[prevSlice]) {
                        // insert the current point into prevSlice
                        space[pointIndex + 1] = space[prevSlice + 2];
                        space[prevSlice + 2] = pointIndex;
                    } else {
                        sliceCurrent += 3;
                        // create a new slice and insert it between prevSlice and nextSlice
                        initNewSliceA(prevSlice, sliceCurrent, nextSlice, currRank, pointIndex);
                        space[pointIndex + 1] = -1;
                    }
                    pointIndex += 2;
                }
            }
            return JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minOverflow, until);
        }

        private boolean checkWhetherDominates(int goodFrom, int goodUntil, double[] wp, int obj) {
            while (goodUntil > goodFrom) {
                --goodUntil;
                if (DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(exPoints[goodUntil], wp, obj)) {
                    return true;
                }
            }
            return false;
        }

        private int helperBSingleRank(int rank, int goodFrom, int goodUntil,
                                      int weakFrom, int weakUntil, int obj, int maximalMeaningfulRank, int tempFrom) {
            int minUpdated = weakUntil;
            int offset = tempFrom - goodFrom;
            for (int good = goodFrom; good < goodUntil; ++good) {
                exPoints[offset + good] = points[indices[good]];
            }
            for (int weak = weakFrom, good = goodFrom; weak < weakUntil; ++weak) {
                int wi = indices[weak];
                if (ranks[wi] > rank) {
                    continue;
                }
                good = ArrayHelper.findWhereNotSmaller(indices, good, goodUntil, wi);
                if (checkWhetherDominates(tempFrom, good + offset, points[wi], obj)) {
                    ranks[wi] = rank + 1;
                    if (minUpdated > weak) {
                        minUpdated = weak;
                    }
                }
            }
            Arrays.fill(exPoints, tempFrom, tempFrom + goodUntil - goodFrom, null);
            return rank == maximalMeaningfulRank && minUpdated < weakUntil
                    ? JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minUpdated, weakUntil)
                    : weakUntil;
        }

        private int transplantRanksAndCheckWhetherAllAreSame(int goodFrom, int goodUntil, int ranksAndSlicesOffset, int sortedIndicesOffset) {
            int firstRank = -ranks[indices[goodFrom]];
            boolean allSame = true;
            space[ranksAndSlicesOffset] = firstRank;
            space[sortedIndicesOffset] = ranksAndSlicesOffset;
            for (int i = goodFrom + 1, ri = ranksAndSlicesOffset, si = sortedIndicesOffset; i < goodUntil; ++i) {
                ++ri;
                ++si;
                int rank = -ranks[indices[i]];
                allSame &= firstRank == rank;
                space[ri] = rank;
                space[si] = ri;
            }
            return allSame ? firstRank : 1;
        }

        private static int distributePointsBetweenSlices(int[] space, int from, int until, int sliceOffset, int pointsBySlicesOffset) {
            int sliceLast = sliceOffset - 2;
            int atSliceLast = 0;
            int prevRank = 1;
            int sliceRankIndex = from - 1;
            for (int i = from; i < until; ++i) {
                int currIndex = space[i];
                int currRank = space[currIndex];
                if (prevRank != currRank) {
                    prevRank = currRank;
                    if (sliceLast >= sliceOffset) {
                        space[sliceLast] = atSliceLast;
                        atSliceLast = 0;
                    }
                    space[++sliceRankIndex] = currRank;
                    sliceLast += 2;
                }
                ++atSliceLast;
                space[currIndex] = sliceLast;
            }
            space[sliceLast] = atSliceLast;
            for (int i = sliceOffset, collected = pointsBySlicesOffset; i <= sliceLast; i += 2) {
                int current = space[i];
                space[i] = collected;
                space[i + 1] = collected;
                collected += current;
            }
            return sliceLast;
        }

        private int findRankInSlices(int sliceOffset, int sliceLast, int wi, int obj, int sliceRankOffset) {
            int currSlice = sliceLast;
            int sliceRankIndex = ((currSlice - sliceOffset) >>> 1) + sliceRankOffset;
            int weakRank = ranks[wi];
            double[] wp = points[wi];
            while (currSlice >= sliceOffset) {
                int from = space[currSlice];
                int until = space[currSlice + 1];
                if (from < until) {
                    int currRank = -space[sliceRankIndex];
                    if (currRank >= weakRank) {
                        if (checkWhetherDominates(from, until, wp, obj)) {
                            weakRank = currRank + 1;
                        } else {
                            break;
                        }
                    }
                }
                currSlice -= 2;
                --sliceRankIndex;
            }
            return ranks[wi] = weakRank;
        }

        @Override
        public int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom, int maximalMeaningfulRank) {
            int goodSize = goodUntil - goodFrom;
            if (notHookCondition(goodSize + weakUntil - weakFrom, obj)) {
                return -1;
            }

            int sortedIndicesOffset = tempFrom * STORAGE_MULTIPLE;
            int ranksAndSlicesOffset = sortedIndicesOffset + goodSize;
            int sliceOffset = ranksAndSlicesOffset + goodSize;

            int minRank = transplantRanksAndCheckWhetherAllAreSame(goodFrom, goodUntil, ranksAndSlicesOffset, sortedIndicesOffset);
            if (minRank != 1) {
                // "good" has a single front, let's do the simple stuff
                return helperBSingleRank(-minRank, goodFrom, goodUntil, weakFrom, weakUntil, obj, maximalMeaningfulRank, tempFrom);
            } else {
                // "good" has multiple fronts (called "slices" here), need to go a more complicated way.
                ArraySorter.sortIndicesByValues(space, space, sortedIndicesOffset, sortedIndicesOffset + goodSize);
                int sliceLast = distributePointsBetweenSlices(space, sortedIndicesOffset, sortedIndicesOffset + goodSize, sliceOffset, tempFrom);
                int minOverflowed = weakUntil;
                for (int weak = weakFrom, good = goodFrom, sliceOfGood = ranksAndSlicesOffset; weak < weakUntil; ++weak) {
                    int wi = indices[weak];
                    int gi;
                    while (good < goodUntil && (gi = indices[good]) < wi) {
                        int sliceTailIndex = space[sliceOfGood] + 1;
                        int spaceAtTail = space[sliceTailIndex];
                        exPoints[spaceAtTail] = points[gi];
                        space[sliceTailIndex] = spaceAtTail + 1;
                        ++good;
                        ++sliceOfGood;
                    }
                    int weakRank = findRankInSlices(sliceOffset, sliceLast, wi, obj, sortedIndicesOffset);
                    if (weakRank > maximalMeaningfulRank && minOverflowed > weak) {
                        minOverflowed = weak;
                    }
                }
                Arrays.fill(exPoints, tempFrom, tempFrom + goodUntil - goodFrom, null);
                return JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minOverflowed, weakUntil);
            }
        }
    }
}
