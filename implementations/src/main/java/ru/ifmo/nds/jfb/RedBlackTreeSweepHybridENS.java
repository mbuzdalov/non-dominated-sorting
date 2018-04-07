package ru.ifmo.nds.jfb;

public class RedBlackTreeSweepHybridENS extends RedBlackTreeSweep {
    private static final int THRESHOLD_3D = 100;
    private static final int THRESHOLD_ALL = 200;
    private static final int STORAGE_MULTIPLE = 5;

    private int[] space;

    public RedBlackTreeSweepHybridENS(int maximumPoints, int maximumDimension, int allowedThreads) {
        super(maximumPoints, maximumDimension, allowedThreads);
        space = new int[maximumPoints * STORAGE_MULTIPLE];
    }

    @Override
    protected void closeImpl() {
        super.closeImpl();
        space = null;
    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov sorting, " + getThreadDescription() + " (tree sweep, hybrid with ENS)";
    }

    @Override
    protected boolean helperAHookCondition(int size, int obj) {
        switch (obj) {
            case 1: return false;
            case 2: return size < THRESHOLD_3D;
            default: return size < THRESHOLD_ALL;
        }
    }

    private boolean checkIfDominatesA(int sliceIndex, int obj, int weakIndex) {
        int sliceRank = space[sliceIndex];
        if (ranks[weakIndex] > sliceRank) {
            return true;
        }
        int virtualGoodIndex = space[sliceIndex + 2];
        while (virtualGoodIndex != -1) {
            int realGoodIndex = space[virtualGoodIndex];
            if (strictlyDominatesAssumingNotSame(realGoodIndex, weakIndex, obj)) {
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
    protected int helperAHook(int from, int until, int obj) {
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
        return kickOutOverflowedRanks(minOverflow, until);
    }

    @Override
    protected boolean helperBHookCondition(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        int size = goodUntil - goodFrom + weakUntil - weakFrom;
        switch (obj) {
            case 1: return false;
            case 2: return size < THRESHOLD_3D;
            default: return size < THRESHOLD_ALL;
        }
    }

    private void sortIndicesByRanks(int from, int to) {
        int left = from, right = to;
        int pivot = (space[space[from]] + space[space[to]]) / 2;
        while (left <= right) {
            while (space[space[left]] < pivot) ++left;
            while (space[space[right]] > pivot) --right;
            if (left <= right) {
                int tmp = space[left];
                space[left] = space[right];
                space[right] = tmp;
                ++left;
                --right;
            }
        }
        if (from < right) {
            sortIndicesByRanks(from, right);
        }
        if (left < to) {
            sortIndicesByRanks(left, to);
        }
    }

    private boolean checkWhetherDominates(int[] array, int goodFrom, int goodUntil, int weakIndex, int obj) {
        for (int good = goodUntil - 1; good >= goodFrom; --good) {
            int goodIndex = array[good];
            if (strictlyDominatesAssumingNotSame(goodIndex, weakIndex, obj)) {
                return true;
            }
        }
        return false;
    }

    private int helperBSingleRank(int rank, int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        int minUpdated = weakUntil;
        for (int weak = weakFrom, good = goodFrom; weak < weakUntil; ++weak) {
            int wi = indices[weak];
            while (good < goodUntil && indices[good] < wi) {
                ++good;
            }
            if (ranks[wi] <= rank && checkWhetherDominates(indices, goodFrom, good, wi, obj)) {
                ranks[wi] = rank + 1;
                minUpdated = weak;
            }
        }
        return rank == maximalMeaningfulRank ? kickOutOverflowedRanks(minUpdated, weakUntil) : weakUntil;
    }

    @Override
    protected int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom) {
        if (goodFrom == goodUntil || weakFrom == weakUntil) {
            return weakUntil;
        }
        int goodSize = goodUntil - goodFrom;

        int sortedIndicesOffset = tempFrom * STORAGE_MULTIPLE;
        int ranksAndSlicesOffset = sortedIndicesOffset + goodSize;
        int sliceOffset = ranksAndSlicesOffset + goodSize;
        int pointsBySlicesOffset = sliceOffset + 2 * goodSize;

        int minRank = Integer.MAX_VALUE, maxRank = Integer.MIN_VALUE;
        for (int i = goodFrom, ri = ranksAndSlicesOffset, si = sortedIndicesOffset; i < goodUntil; ++i, ++ri, ++si) {
            int rank = ranks[indices[i]];
            if (minRank > rank) {
                minRank = rank;
            }
            if (maxRank < rank) {
                maxRank = rank;
            }
            space[ri] = rank;
            space[si] = ri;
        }

        if (minRank == maxRank) {
            // single front, let's do the simple stuff
            return helperBSingleRank(minRank, goodFrom, goodUntil, weakFrom, weakUntil, obj);
        } else {
            sortIndicesByRanks(sortedIndicesOffset, sortedIndicesOffset + goodSize - 1);
            int sliceLast = sliceOffset - 2;
            int prevRank = -1;
            for (int i = 0; i < goodSize; ++i) {
                int currIndex = space[sortedIndicesOffset + i];
                int currRank = space[currIndex];
                if (prevRank != currRank) {
                    prevRank = currRank;
                    sliceLast += 2;
                    space[sliceLast] = 0;
                }
                ++space[sliceLast];
                space[currIndex] = sliceLast;
            }
            for (int i = sliceOffset, collected = pointsBySlicesOffset; i <= sliceLast; i += 2) {
                int current = space[i];
                space[i] = collected;
                space[i + 1] = collected;
                collected += current;
            }

            int minOverflowed = weakUntil;
            for (int weak = weakFrom, good = goodFrom; weak < weakUntil; ++weak) {
                int wi = indices[weak];
                int gi;
                while (good < goodUntil && (gi = indices[good]) < wi) {
                    int sliceIndex = space[ranksAndSlicesOffset + good - goodFrom];
                    space[space[sliceIndex + 1]++] = gi;
                    ++good;
                }
                int currSlice = sliceOffset;
                int weakRank = ranks[wi];
                while (currSlice <= sliceLast) {
                    int from = space[currSlice];
                    int until = space[currSlice + 1];
                    if (from == until) {
                        currSlice += 2;
                    } else {
                        int currRank = ranks[space[from]];
                        if (currRank < weakRank) {
                            currSlice += 2;
                        } else if (checkWhetherDominates(space, from, until, wi, obj)) {
                            currSlice += 2;
                            weakRank = currRank + 1;
                        } else {
                            break;
                        }
                    }
                }
                ranks[wi] = weakRank;
                if (minOverflowed > weak && weakRank > maximalMeaningfulRank) {
                    minOverflowed = weak;
                }
            }
            return kickOutOverflowedRanks(minOverflowed, weakUntil);
        }
    }
}
