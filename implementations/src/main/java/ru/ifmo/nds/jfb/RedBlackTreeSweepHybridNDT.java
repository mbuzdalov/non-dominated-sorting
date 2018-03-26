package ru.ifmo.nds.jfb;

import ru.ifmo.nds.ndt.ENS_NDT_AdaptedForHybrid;

public class RedBlackTreeSweepHybridNDT extends RedBlackTreeSweep {
    private ENS_NDT_AdaptedForHybrid ndtSorter;

    private double[][] tempPoints;
    private int[] tempRanks;

    private static final int THRESHOLD_3D = 100;
    private static final int THRESHOLD_ALL = 20000;

    public RedBlackTreeSweepHybridNDT(int maximumPoints, int maximumDimension, int allowedThreads, int threshold) {
        super(maximumPoints, maximumDimension, allowedThreads);

        tempPoints = new double[maximumPoints][maximumDimension];
        tempRanks = new int[maximumPoints];
        ndtSorter = new ENS_NDT_AdaptedForHybrid(maximumPoints, maximumDimension, threshold);
    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov sorting, " + getThreadDescription() + " (tree sweep, hybrid with " + ndtSorter.getName() + ")";
    }

    @Override
    protected boolean helperAHookCondition(int size, int obj) {
        switch (obj) {
            case 1: return false;
            case 2: return size < THRESHOLD_3D;
            default: return size < THRESHOLD_ALL;
        }
    }

    @Override
    protected boolean helperBHookCondition(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        return helperAHookCondition(goodUntil - goodFrom + weakUntil - weakFrom, obj);
    }

    @Override
    protected int helperAHook(int from, int until, int obj) {
        getPoints(from, until, obj + 1, tempPoints, from);

        getRanks(from, until, tempRanks, from);

        ndtSorter.sortHelperA(tempPoints, tempRanks, from, until, obj + 1, maximalMeaningfulRank);

        for (int i = from; i < until; i++) {
            ranks[indices[i]] = tempRanks[i];
        }

        return kickOutOverflowedRanks(from, until);
    }


    @Override
    protected int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom) {
        getPoints(goodFrom, goodUntil, obj + 1, tempPoints, goodFrom);
        getPoints(weakFrom, weakUntil, obj + 1, tempPoints, weakFrom);

        getRanks(goodFrom, goodUntil, tempRanks, goodFrom);
        getRanks(weakFrom, weakUntil, tempRanks, weakFrom);

        ndtSorter.sortHelperB(tempPoints, tempRanks, goodFrom, goodUntil, weakFrom, weakUntil, obj + 1, maximalMeaningfulRank);

        for (int i = weakFrom; i < weakUntil; i++) {
            ranks[indices[i]] = tempRanks[i];
        }

        return kickOutOverflowedRanks(weakFrom, weakUntil);
    }
}
