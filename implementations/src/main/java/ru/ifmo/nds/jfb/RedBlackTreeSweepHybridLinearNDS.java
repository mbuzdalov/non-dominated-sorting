package ru.ifmo.nds.jfb;

public class RedBlackTreeSweepHybridLinearNDS extends RedBlackTreeSweep {
    private int[] badGuys;

    public RedBlackTreeSweepHybridLinearNDS(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        badGuys = new int[maximumPoints];
    }

    @Override
    protected void closeImpl() throws Exception {
        super.closeImpl();
        badGuys = null;
    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov sorting (tree sweep, hybrid with fast NDS)";
    }

    @Override
    protected boolean helperAHookCondition(int size, int obj) {
        switch (obj) {
            case 2: return false;
            case 3: return size < 70;
            default: return size < 400;
        }
    }

    @Override
    protected int helperAHook(int from, int until, int obj) {
        int badCount = 0;
        for (int left = from; left < until; ++left) {
            int leftIndex = indices[left];
            int leftRank = ranks[leftIndex];
            int newUntil = left + 1;
            for (int right = left + 1; right < until; ++right) {
                int rightIndex = indices[right];
                if (ranks[rightIndex] <= leftRank && strictlyDominatesAssumingNotSame(leftIndex, rightIndex, obj)) {
                    if (leftRank == maximalMeaningfulRank) {
                        reportOverflowedRank(rightIndex);
                        badGuys[badCount++] = rightIndex;
                        continue;
                    } else {
                        ranks[rightIndex] = leftRank + 1;
                    }
                }
                indices[newUntil++] = rightIndex;
            }
            until = newUntil;
        }
        System.arraycopy(badGuys, 0, indices, until, badCount);
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
