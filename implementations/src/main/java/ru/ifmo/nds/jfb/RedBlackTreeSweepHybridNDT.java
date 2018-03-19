package ru.ifmo.nds.jfb;

public class RedBlackTreeSweepHybridNDT extends RedBlackTreeSweep {

    public RedBlackTreeSweepHybridNDT(int maximumPoints, int maximumDimension, int allowedThreads) {
        super(maximumPoints, maximumDimension, allowedThreads);
    }

    @Override
    protected boolean helperBHookCondition(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        return false;
    }

    @Override
    protected int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom) {
        return -1;
    }
}
