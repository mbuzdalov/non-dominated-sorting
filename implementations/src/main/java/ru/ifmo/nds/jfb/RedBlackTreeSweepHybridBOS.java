package ru.ifmo.nds.jfb;

import ru.ifmo.nds.bos.ImprovedAdaptedForHybrid;

import java.util.Random; // TODO delete

public class RedBlackTreeSweepHybridBOS extends RedBlackTreeSweep {
    private final ImprovedAdaptedForHybrid bos;
    private double[][] tempPoints;
    private int[] tempRanks;
    private Random random = new Random(239); // TODO delete

    private static final int THRESHOLD_3D = 100;
    private static final int THRESHOLD_ALL = 200;

    public RedBlackTreeSweepHybridBOS(int maximumPoints, int maximumDimension, int allowedThreads) {
        super(maximumPoints, maximumDimension, allowedThreads);
        bos = new ImprovedAdaptedForHybrid(maximumPoints, maximumDimension);
        tempPoints = new double[maximumPoints][maximumDimension];
        tempRanks = new int[maximumPoints];
    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov sorting, "
                + getThreadDescription()
                + " (tree sweep, hybrid with Best Order Sort)";
    }

    @Override
    protected boolean helperAHookCondition(int size, int obj) {
        switch (obj) {
            case 1:
                return false;
            case 2:
                return size < THRESHOLD_3D;
            default:
                return size < THRESHOLD_ALL;
        }
    }

    @Override
    protected boolean helperBHookCondition(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        return helperAHookCondition(goodUntil - goodFrom + weakUntil - weakFrom, obj);
    }

    @Override
    protected int helperAHook(int from, int until, int obj) {
        getPoints(from, until, obj + 1, tempPoints, 0);
        getRanks(from, until, tempRanks, 0);

        bos.sortCheckedWithRespectToRanks(
                tempPoints,
                tempRanks,
                until - from,
                obj + 1,
                maximalMeaningfulRank);

        for (int i = from; i < until; i++) {
            ranks[indices[i]] = tempRanks[i - from];
        }
        return kickOutOverflowedRanks(from, until);
    }

    @Override
    protected int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom) {
        getPoints(goodFrom, goodUntil, obj + 1, tempPoints, goodFrom);
        getPoints(weakFrom, weakUntil, obj + 1, tempPoints, weakFrom);

        getRanks(goodFrom, goodUntil, tempRanks, goodFrom);
        getRanks(weakFrom, weakUntil, tempRanks, weakFrom);

        bos.sortCheckedWithRespectToRanksHelperB(
                tempPoints,
                tempRanks,
                goodFrom,
                goodUntil,
                weakFrom,
                weakUntil,
                obj + 1,
                maximalMeaningfulRank);

        for (int i = weakFrom; i < weakUntil; i++) {
            ranks[indices[i]] = tempRanks[i];
        }

        return kickOutOverflowedRanks(weakFrom, weakUntil);
    }
}

