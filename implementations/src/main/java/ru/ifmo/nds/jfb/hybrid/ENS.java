package ru.ifmo.nds.jfb.hybrid;

import ru.ifmo.nds.jfb.HybridAlgorithmWrapper;
import ru.ifmo.nds.jfb.JFBBase;
import ru.ifmo.nds.util.DominanceHelper;

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

        private final int threshold3D;
        private final int thresholdAll;

        private Instance(int[] ranks, int[] indices, double[][] points, int threshold3D, int thresholdAll) {
            this.ranks = ranks;
            this.indices = indices;
            this.points = points;
            this.space = new int[STORAGE_MULTIPLE * indices.length];
            this.threshold3D = threshold3D;
            this.thresholdAll = thresholdAll;
        }

        @Override
        public boolean helperAHookCondition(int size, int obj) {
            switch (obj) {
                case 1:
                    return false;
                case 2:
                    return size < threshold3D;
                default:
                    return size < thresholdAll;
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

        @Override
        public boolean helperBHookCondition(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
            int size = goodUntil - goodFrom + weakUntil - weakFrom;
            switch (obj) {
                case 1:
                    return false;
                case 2:
                    return size < threshold3D;
                default:
                    return size < thresholdAll;
            }
        }

        private static int splitIndicesByRanks(int[] space, int from, int to) {
            int left = from, right = to;
            int pivot = (space[space[from]] + space[space[to]]) / 2;
            int sl, sr;
            while (left <= right) {
                while (space[sl = space[left]] > pivot) ++left;
                while (space[sr = space[right]] < pivot) --right;
                if (left <= right) {
                    space[left] = sr;
                    space[right] = sl;
                    ++left;
                    --right;
                }
            }
            return left - 1 == right ? left : -left - 1;
        }

        private static final int INSERTION_THRESHOLD = 20;

        private static void insertionSortIndicesByRank(int[] space, int from, int to) {
            for (int i = from, j = i; i < to; j = ++i) {
                int ii = space[i + 1];
                int ai = space[ii];
                while (ai > space[space[j]]) {
                    space[j + 1] = space[j];
                    if (j-- == from) {
                        break;
                    }
                }
                space[j + 1] = ii;
            }
        }

        private static void sortIndicesByRanks(int[] space, int from, int to) {
            if (from + INSERTION_THRESHOLD > to) {
                insertionSortIndicesByRank(space, from, to);
            } else {
                int left = splitIndicesByRanks(space, from, to);
                int right = left - 1;
                if (left < 0) {
                    left = -left - 1;
                    right = left - 2;
                }
                if (from < right) {
                    sortIndicesByRanks(space, from, right);
                }
                if (left < to) {
                    sortIndicesByRanks(space, left, to);
                }
            }
        }

        private boolean checkWhetherDominates(int[] array, int goodFrom, int goodUntil, double[] wp, int obj) {
            while (goodUntil > goodFrom) {
                --goodUntil;
                if (DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(points[array[goodUntil]], wp, obj)) {
                    return true;
                }
            }
            return false;
        }

        private int helperBSingleRank(int rank, int goodFrom, int goodUntil,
                                      int weakFrom, int weakUntil, int obj, int maximalMeaningfulRank) {
            int minUpdated = weakUntil;
            for (int weak = weakFrom, good = goodFrom; weak < weakUntil; ++weak) {
                int wi = indices[weak];
                if (ranks[wi] > rank) {
                    continue;
                }
                while (good < goodUntil && indices[good] < wi) {
                    ++good;
                }
                if (checkWhetherDominates(indices, goodFrom, good, points[wi], obj)) {
                    ranks[wi] = rank + 1;
                    if (minUpdated > weak) {
                        minUpdated = weak;
                    }
                }
            }
            return rank == maximalMeaningfulRank && minUpdated < weakUntil
                    ? JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minUpdated, weakUntil)
                    : weakUntil;
        }

        private int transplantRanksAndCheckWhetherAllAreSame(int goodFrom, int goodUntil, int ranksAndSlicesOffset, int sortedIndicesOffset) {
            int firstRank = ranks[indices[goodFrom]];
            boolean allSame = true;
            space[ranksAndSlicesOffset] = firstRank;
            space[sortedIndicesOffset] = ranksAndSlicesOffset;
            for (int i = goodFrom + 1, ri = ranksAndSlicesOffset, si = sortedIndicesOffset; i < goodUntil; ++i) {
                ++ri;
                ++si;
                int rank = ranks[indices[i]];
                allSame &= firstRank == rank;
                space[ri] = rank;
                space[si] = ri;
            }
            return allSame ? firstRank : -1;
        }

        private static int distributePointsBetweenSlices(int[] space, int from, int until, int sliceOffset, int pointsBySlicesOffset) {
            int sliceLast = sliceOffset - 2;
            int atSliceLast = 0;
            int prevRank = -1;
            for (int i = from; i < until; ++i) {
                int currIndex = space[i];
                int currRank = space[currIndex];
                if (prevRank != currRank) {
                    prevRank = currRank;
                    if (sliceLast >= sliceOffset) {
                        space[sliceLast] = atSliceLast;
                        atSliceLast = 0;
                    }
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

        private int findRankInSlices(int sliceOffset, int sliceLast, int wi, int obj) {
            int currSlice = sliceLast;
            int weakRank = ranks[wi];
            double[] wp = points[wi];
            while (currSlice >= sliceOffset) {
                int from = space[currSlice];
                int until = space[currSlice + 1];
                if (from < until) {
                    int currRank = ranks[space[until - 1]];
                    if (currRank >= weakRank) {
                        if (checkWhetherDominates(space, from, until, wp, obj)) {
                            weakRank = currRank + 1;
                        } else {
                            break;
                        }
                    }
                }
                currSlice -= 2;
            }
            return ranks[wi] = weakRank;
        }

        @Override
        public int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom, int maximalMeaningfulRank) {
            int goodSize = goodUntil - goodFrom;

            int sortedIndicesOffset = tempFrom * STORAGE_MULTIPLE;
            int ranksAndSlicesOffset = sortedIndicesOffset + goodSize;
            int sliceOffset = ranksAndSlicesOffset + goodSize;
            int pointsBySlicesOffset = sliceOffset + 2 * goodSize;

            int minRank = transplantRanksAndCheckWhetherAllAreSame(goodFrom, goodUntil, ranksAndSlicesOffset, sortedIndicesOffset);
            if (minRank != -1) {
                // "good" has a single front, let's do the simple stuff
                return helperBSingleRank(minRank, goodFrom, goodUntil, weakFrom, weakUntil, obj, maximalMeaningfulRank);
            } else {
                // "good" has multiple fronts (called "slices" here), need to go a more complicated way.
                sortIndicesByRanks(space, sortedIndicesOffset, sortedIndicesOffset + goodSize - 1);
                int sliceLast = distributePointsBetweenSlices(space, sortedIndicesOffset, sortedIndicesOffset + goodSize, sliceOffset, pointsBySlicesOffset);
                int minOverflowed = weakUntil;
                for (int weak = weakFrom, good = goodFrom, sliceOfGood = ranksAndSlicesOffset; weak < weakUntil; ++weak) {
                    int wi = indices[weak];
                    int gi;
                    while (good < goodUntil && (gi = indices[good]) < wi) {
                        int sliceTailIndex = space[sliceOfGood] + 1;
                        int spaceAtTail = space[sliceTailIndex];
                        space[spaceAtTail] = gi;
                        space[sliceTailIndex] = spaceAtTail + 1;
                        ++good;
                        ++sliceOfGood;
                    }
                    int weakRank = findRankInSlices(sliceOffset, sliceLast, wi, obj);
                    if (weakRank > maximalMeaningfulRank && minOverflowed > weak) {
                        minOverflowed = weak;
                    }
                }
                return JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minOverflowed, weakUntil);
            }
        }
    }
}
