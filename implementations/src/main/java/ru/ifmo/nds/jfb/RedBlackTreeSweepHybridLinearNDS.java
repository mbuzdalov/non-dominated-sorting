package ru.ifmo.nds.jfb;

public class RedBlackTreeSweepHybridLinearNDS extends RedBlackTreeSweep {
    private int[] badGuys;

    public RedBlackTreeSweepHybridLinearNDS(int maximumPoints, int maximumDimension, boolean useRankFilter) {
        super(maximumPoints, maximumDimension, useRankFilter);
        badGuys = new int[maximumPoints];
    }

    @Override
    protected void closeImpl() throws Exception {
        super.closeImpl();
        badGuys = null;
    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov sorting (tree sweep, hybrid with fast NDS"
                + (useRankFilter ? ", rank filter" : "") + ")";
    }

    @Override
    protected boolean helperAHookCondition(int size, int obj) {
        switch (obj) {
            case 1: return false;
            case 2: return size < 70;
            default: return size < 400;
        }
    }

    private int updateByPointWithMove(int pointIndex, int from, int until, int obj) {
        int badCount = 0;
        reportOverflowedRank(indices[from]);
        badGuys[badCount++] = indices[from];
        int newUntil = from;
        for (int i = from + 1; i < until; ++i) {
            int ii = indices[i];
            if (ranks[ii] <= maximalMeaningfulRank && strictlyDominatesAssumingNotSame(pointIndex, ii, obj)) {
                reportOverflowedRank(ii);
                badGuys[badCount++] = ii;
            } else {
                indices[newUntil++] = ii;
            }
        }
        return newUntil;
    }

    private void updateByPoint(int pointIndex, int pointRank, int from, int until, int obj) {
        for (int i = from; i < until; ++i) {
            int ii = indices[i];
            if (ranks[ii] <= pointRank && strictlyDominatesAssumingNotSame(pointIndex, ii, obj)) {
                ranks[ii] = pointRank + 1;
            }
        }
    }

    private int updateByPointCritical(int pointIndex, int from, int until, int obj) {
        for (int i = from; i < until; ++i) {
            int ii = indices[i];
            if (ranks[ii] <= maximalMeaningfulRank && strictlyDominatesAssumingNotSame(pointIndex, ii, obj)) {
                return updateByPointWithMove(pointIndex, i, until, obj);
            }
        }
        return until;
    }

    @Override
    protected int helperAHook(int from, int until, int obj) {
        int oldUntil = until;
        for (int left = from; left < until; ++left) {
            int leftIndex = indices[left];
            int leftRank = ranks[leftIndex];
            if (leftRank < maximalMeaningfulRank) {
                updateByPoint(leftIndex, leftRank, left + 1, until, obj);
            } else {
                until = updateByPointCritical(leftIndex, left + 1, until, obj);
            }
        }
        System.arraycopy(badGuys, 0, indices, until, oldUntil - until);
        return until;
    }

    @Override
    protected boolean helperBHookCondition(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        return helperAHookCondition(goodUntil - goodFrom + weakUntil - weakFrom, obj);
    }

    @Override
    protected int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        for (int good = goodFrom, weakMin = weakFrom; good < goodUntil; ++good) {
            int goodIndex = indices[good];
            int goodRank = ranks[goodIndex];
            while (weakMin < weakUntil && indices[weakMin] < goodIndex) {
                ++weakMin;
            }
            int newWeakUntil = weakMin;
            for (int weak = weakMin; weak < weakUntil; ++weak) {
                int weakIndex = indices[weak];
                if (goodRank >= ranks[weakIndex] && strictlyDominatesAssumingNotSame(goodIndex, weakIndex, obj)) {
                    ranks[weakIndex] = 1 + goodRank;
                    if (ranks[weakIndex] > maximalMeaningfulRank) {
                        reportOverflowedRank(weakIndex);
                        continue;
                    }
                }
                indices[newWeakUntil++] = weakIndex;
            }
            weakUntil = newWeakUntil;
        }
        return weakUntil;
    }
}
