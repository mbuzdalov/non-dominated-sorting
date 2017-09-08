package ru.ifmo.nds.jfb;

public class RedBlackTreeSweepHybridLinearNDS extends RedBlackTreeSweep {
    public RedBlackTreeSweepHybridLinearNDS(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    protected void closeImpl() throws Exception {
        super.closeImpl();
    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov sorting (tree sweep, hybrid with fast NDS)";
    }

    @Override
    protected boolean helperAHookCondition(int size, int obj) {
        switch (obj) {
            case 1: return false;
            case 2: return size < 70;
            default: return size < 400;
        }
    }

    @Override
    protected int helperAHook(int from, int until, int obj) {
        for (int left = from; left < until; ++left) {
            until = updateByPoint(indices[left], left + 1, until, obj);
        }
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
            while (weakMin < weakUntil && indices[weakMin] < goodIndex) {
                ++weakMin;
            }
            weakUntil = updateByPoint(goodIndex, weakMin, weakUntil, obj);
        }
        return weakUntil;
    }
}
