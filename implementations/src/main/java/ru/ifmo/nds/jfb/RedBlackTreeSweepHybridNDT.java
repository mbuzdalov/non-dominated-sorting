package ru.ifmo.nds.jfb;

import ru.ifmo.nds.ndt.ENS_NDT_AdaptedForHybrid;

public class RedBlackTreeSweepHybridNDT extends RedBlackTreeSweep {
    private ENS_NDT_AdaptedForHybrid ndtSorter;

    private double[][] tempPoints;
    private int[] tempRanks;

    public RedBlackTreeSweepHybridNDT(int maximumPoints, int maximumDimension, int allowedThreads) {
        super(maximumPoints, maximumDimension, allowedThreads);

        tempPoints = new double[maximumPoints][maximumDimension];
        tempRanks = new int[maximumPoints];
        ndtSorter = new ENS_NDT_AdaptedForHybrid(maximumPoints, maximumDimension, 8);
    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov sorting, " + getThreadDescription() + " (tree sweep, hybrid with " + ndtSorter.getName() + ")";
    }

    @Override
    protected boolean helperBHookCondition(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        return true;
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
