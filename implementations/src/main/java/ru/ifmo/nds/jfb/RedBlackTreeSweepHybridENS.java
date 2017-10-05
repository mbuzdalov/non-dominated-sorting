package ru.ifmo.nds.jfb;

public class RedBlackTreeSweepHybridENS extends RedBlackTreeSweep {
    private static final int MAX_SIZE = 400;

    private int[] sliceRank;
    private int[] sliceSize;
    private int[] sliceNext;
    private int[] slicePoint;
    private int[] pointIndex;
    private int[] pointNext;

    public RedBlackTreeSweepHybridENS(int maximumPoints, int maximumDimension, int allowedThreads) {
        super(maximumPoints, maximumDimension, allowedThreads);
        slicePoint = new int[maximumPoints];
        sliceNext = new int[maximumPoints];
        sliceSize = new int[maximumPoints];
        sliceRank = new int[maximumPoints];
        pointIndex = new int[maximumPoints];
        pointNext = new int[maximumPoints];
    }
    @Override
    protected void closeImpl() throws Exception {
        super.closeImpl();
        slicePoint = null;
        sliceNext = null;
        sliceSize = null;
        sliceRank = null;
        pointIndex = null;
        pointNext = null;
    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov sorting, " + getThreadDescription() + " (tree sweep, hybrid with ENS)";
    }

    @Override
    protected boolean helperAHookCondition(int size, int obj) {
        switch (obj) {
            case 1: return false;
            case 2: return size < 100;
            default: return size < MAX_SIZE;
        }
    }

    private boolean checkIfDominatesA(int index, int obj, int slice) {
        if (ranks[index] > sliceRank[slice]) {
            return true;
        }
        int size = sliceSize[slice];
        int point = slicePoint[slice];
        for (int i = 0; i < size; ++i, point = pointNext[point]) {
            if (strictlyDominatesAssumingNotSame(pointIndex[point], index, obj)) {
                ranks[index] = 1 + sliceRank[slice];
                return true;
            }
        }
        return false;
    }

    private int initNewSliceA(int sliceAddress, int nextSlice, int pointIndex, int pointAddress) {
        sliceRank[sliceAddress] = ranks[pointIndex];
        sliceSize[sliceAddress] = 1;
        sliceNext[sliceAddress] = nextSlice;
        slicePoint[sliceAddress] = pointAddress;
        pointNext[pointAddress] = -1;

        return sliceAddress;
    }

    @Override
    protected int helperAHook(int from, int until, int obj) {
        int sliceCount = 0;
        int sliceFirst = -1;

        int minOverflow = until;
        for (int i = from, pointCount = 0; i < until; ++i) {
            int ii = indices[i];
            if (sliceFirst == -1 || checkIfDominatesA(ii, obj, sliceFirst)) {
                // the current point forms its own slice, which will have the maximum known rank.
                if (ranks[ii] <= maximalMeaningfulRank) {
                    int pointAddress = from + pointCount++;
                    pointIndex[pointAddress] = ii;
                    sliceFirst = initNewSliceA(from + sliceCount++, sliceFirst, ii, pointAddress);
                } else if (minOverflow > i) {
                    minOverflow = i;
                }
            } else {
                int prevSlice = sliceFirst, nextSlice = sliceNext[sliceFirst];
                while (nextSlice != -1 && !checkIfDominatesA(ii, obj, nextSlice)) {
                    prevSlice = nextSlice;
                    nextSlice = sliceNext[nextSlice];
                }

                // our point is dominated by nextSlice (or is the best) ...
                int pointAddress = from + pointCount++;
                pointIndex[pointAddress] = ii;
                if (ranks[ii] == sliceRank[prevSlice]) {
                    // ... and its rank is exactly the rank of prevSlice
                    pointNext[pointAddress] = slicePoint[prevSlice];
                    slicePoint[prevSlice] = pointAddress;
                    ++sliceSize[prevSlice];
                } else {
                    // ... and its rank is between prevSlice and nextSlice
                    sliceNext[prevSlice] = initNewSliceA(from + sliceCount++, nextSlice, ii, pointAddress);
                }
            }
        }
        return kickOutOverflowedRanks(minOverflow, until);
    }

    @Override
    protected boolean helperBHookCondition(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        return helperAHookCondition(goodUntil - goodFrom + weakUntil - weakFrom, obj);
    }

    @Override
    protected int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom, int tempUntil) {
        int sliceCount = 0;
        int sliceFirst = -1;

        int minOverflowed = weakUntil;
        for (int gi = goodFrom, wi = weakFrom; wi < weakUntil; ++wi) {
            while (gi < goodUntil && indices[gi] < indices[wi]) {
                int ipr = insertPoint(indices[gi++], gi - goodFrom, tempFrom, sliceFirst, sliceCount);
                if (ipr >= 0) {
                    ++sliceCount;
                    sliceFirst = ipr;
                }
            }
            int ii = indices[wi];
            ranks[ii] = findRank(ii, obj, sliceFirst, sliceCount);
            if (ranks[ii] > maximalMeaningfulRank && minOverflowed > wi) {
                minOverflowed = wi;
            }
        }
        return kickOutOverflowedRanks(minOverflowed, weakUntil);
    }

    private int findRank(int index, int obj, int sliceFirst, int sliceCount) {
        int existingRank = ranks[index];
        for (int slice = sliceFirst, i = 0; i < sliceCount; ++i, slice = sliceNext[slice]) {
            if (sliceRank[slice] < existingRank) {
                return existingRank;
            }
            if (checkIfDominatesA(index, obj, slice)) {
                return sliceRank[slice] + 1;
            }
        }
        return existingRank;
    }

    // Returns:
    //           -1 if sliceCount does not change
    //   sliceFirst if sliceCount changes
    private int insertPoint(int index, final int pointCount, int swapStart, int sliceFirst, int sliceCount) {
        int rank = ranks[index];
        int pointAddress = swapStart + pointCount;
        pointIndex[pointAddress] = index;
        int prevSlice = -1;
        for (int slice = sliceFirst, i = 0; i < sliceCount; ++i, prevSlice = slice, slice = sliceNext[slice]) {
            if (sliceRank[slice] == rank) {
                pointNext[pointAddress] = slicePoint[slice];
                slicePoint[slice] = pointAddress;
                ++sliceSize[slice];
                return -1;
            }
            if (sliceRank[slice] < rank) {
                // create new slice between prevSlice and slice
                break;
            }
        }
        pointNext[pointAddress] = -1;
        // creating new slice
        int sliceAddress = swapStart + sliceCount;
        sliceRank[sliceAddress] = rank;
        sliceSize[sliceAddress] = 1;
        slicePoint[sliceAddress] = pointAddress;
        if (prevSlice != -1) {
            sliceNext[sliceAddress] = sliceNext[prevSlice];
            sliceNext[prevSlice] = sliceAddress;
        } else {
            sliceNext[sliceAddress] = sliceFirst;
            sliceFirst = sliceAddress;
        }
        return sliceFirst;
    }
}
