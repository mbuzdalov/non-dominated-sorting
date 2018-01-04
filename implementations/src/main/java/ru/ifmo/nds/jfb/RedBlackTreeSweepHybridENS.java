package ru.ifmo.nds.jfb;

public class RedBlackTreeSweepHybridENS extends RedBlackTreeSweep {
    private static final int THRESHOLD_3D = 30;
    private static final int THRESHOLD_ALL = 100;

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
    protected void closeImpl() {
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
            case 2: return size < THRESHOLD_3D;
            default: return size < THRESHOLD_ALL;
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

    private void quickSortByRankIndex(int from, int to) {
        int pivot = pointNext[pointIndex[(from + to) >>> 1]];
        int pivotFirst = from, greaterFirst = to;

        for (int i = from; i <= greaterFirst; ++i) {
            int pi = pointIndex[i];
            int value = pointNext[pi];
            if (value == pivot) {
                continue;
            }
            if (value < pivot) {
                pointIndex[i] = pointIndex[pivotFirst];
                pointIndex[pivotFirst++] = pi;
            } else {
                int pig = pointIndex[greaterFirst];
                while (pointNext[pig] > pivot) {
                    pig = pointIndex[--greaterFirst];
                }
                if (pointNext[pig] == pivot) {
                    pointIndex[i] = pig;
                } else {
                    pointIndex[i] = pointIndex[pivotFirst];
                    pointIndex[pivotFirst++] = pig;
                }
                pointIndex[greaterFirst--] = pi;
            }
        }

        if (from < --pivotFirst) quickSortByRankIndex(from, pivotFirst);
        if (++greaterFirst < to) quickSortByRankIndex(greaterFirst, to);
    }

    @Override
    protected boolean helperBHookCondition(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        return helperAHookCondition(goodUntil - goodFrom + weakUntil - weakFrom, obj);
    }

    private boolean findWhetherDominates(int from, int until, int index, int obj) {
        for (int t = from; t < until; ++t) {
            int ti = pointIndex[t];
            if (strictlyDominatesAssumingNotSame(ti, index, obj)) {
                return true;
            }
        }
        return false;
    }

    private boolean findRank(int sliceLast, int sliceFirst, int index, int existingRank, int obj) {
        for (int slice = sliceLast; slice >= sliceFirst; --slice) {
            int from = sliceSize[slice], until = sliceNext[slice];
            if (from == until) {
                continue;
            }
            int currentRank = ranks[pointIndex[from]];
            if (currentRank < existingRank) {
                break;
            }
            if (findWhetherDominates(from, until, index, obj)) {
                return (ranks[index] = currentRank + 1) > maximalMeaningfulRank;
            }
        }
        return false;
    }

    @Override
    protected int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom, int tempUntil) {
        if (goodFrom == goodUntil || weakFrom == weakUntil) {
            return weakUntil;
        }
        int tempLimit = tempFrom + goodUntil - goodFrom;
        int sliceLast = tempFrom;

        // Big warning: array names below mean really NOTHING.

        // A complicated preparation in O(N log N), for N = goodUntil - goodFrom.
        // First, collect ranks and indices into local arrays.
        for (int gi = goodFrom, ti = tempFrom; gi < goodUntil; ++gi, ++ti) {
            pointNext[ti] = ranks[indices[gi]];
            pointIndex[ti] = ti;
        }
        // Second, sort the index array by the rank array
        quickSortByRankIndex(tempFrom, tempLimit - 1);
        // Third, compute the inverse permutation
        for (int ti = tempFrom; ti < tempLimit; ++ti) {
            slicePoint[pointIndex[ti]] = ti;
        }
        // Fourth, assign slice index to each point and compute slice-last indices
        sliceRank[tempFrom] = sliceLast;
        sliceSize[sliceLast] = tempFrom + 1;
        for (int ti = tempFrom + 1; ti < tempLimit; ++ti) {
            if (pointNext[pointIndex[ti]] != pointNext[pointIndex[ti - 1]]) {
                int prevSize = sliceSize[sliceLast++];
                sliceSize[sliceLast] = prevSize;
            }
            sliceRank[ti] = sliceLast;
            ++sliceSize[sliceLast];
        }
        System.arraycopy(sliceSize, tempFrom, sliceNext, tempFrom, sliceLast + 1 - tempFrom);

        int sliceMax = tempFrom - 1, sliceMin = sliceLast + 1;

        int minOverflowed = weakUntil;
        for (int gi = goodFrom, wi = weakFrom; wi < weakUntil; ++wi) {
            int ii = indices[wi];
            while (gi < goodUntil && indices[gi] < ii) {
                int mySlice = sliceRank[slicePoint[gi - goodFrom + tempFrom]];
                pointIndex[--sliceSize[mySlice]] = indices[gi];
                sliceMax = Math.max(sliceMax, mySlice);
                sliceMin = Math.min(sliceMin, mySlice);
                ++gi;
            }
            int existingRank = ranks[ii];
            if (findRank(sliceMax, sliceMin, ii, existingRank, obj) && minOverflowed > wi) {
                minOverflowed = wi;
            }
        }
        return kickOutOverflowedRanks(minOverflowed, weakUntil);
    }
}
