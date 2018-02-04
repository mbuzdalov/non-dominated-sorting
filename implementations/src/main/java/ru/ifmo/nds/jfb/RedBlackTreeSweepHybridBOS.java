package ru.ifmo.nds.jfb;

public class RedBlackTreeSweepHybridBOS extends RedBlackTreeSweep {
    public RedBlackTreeSweepHybridBOS(int maximumPoints, int maximumDimension, int allowedThreads) {
        super(maximumPoints, maximumDimension, allowedThreads);

    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov sorting, "
                + getThreadDescription()
                + " (tree sweep, hybrid with Best Order Sort)";
    }

    @Override
    protected boolean helperBHookCondition(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        return false; // TODO
    }

    @Override
    protected int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom, int tempUntil) {
        return super.helperBMain(goodFrom, goodUntil, weakFrom, weakUntil, obj, tempFrom, tempUntil);
    }

    @Override
    protected boolean helperAHookCondition(int size, int obj) {
        return false; // TODO
    }

    @Override
    protected int helperAHook(int from, int until, int obj) {
        return super.helperAMain(from, until, obj);
    }
}
