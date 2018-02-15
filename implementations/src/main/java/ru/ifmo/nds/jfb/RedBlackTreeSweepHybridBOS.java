package ru.ifmo.nds.jfb;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.bos.Improved;

import java.util.Random;

public class RedBlackTreeSweepHybridBOS extends RedBlackTreeSweep {
    private final NonDominatedSorting bos;
    private final Random random = new Random(991);

    public RedBlackTreeSweepHybridBOS(int maximumPoints, int maximumDimension, int allowedThreads) {
        super(maximumPoints, maximumDimension, allowedThreads);
        bos = new Improved(maximumPoints, maximumDimension);
    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov sorting, "
                + getThreadDescription()
                + " (tree sweep, hybrid with Best Order Sort)";
    }

    @Override
    protected boolean helperAHookCondition(int size, int obj) {
        return getMaximumPoints() != size && random.nextBoolean(); // TODO fix
    }

    @Override
    protected int helperAHook(int from, int until, int obj) {
        double[][] ps = getPoints(from, until, obj + 1);
        int[] rs = getRanks(from, until);

        bos.sortWithRespectToRanks(ps, rs, maximalMeaningfulRank);

        for (int i = from; i < until; i++) {
            ranks[indices[i]] = rs[i - from];
        }
        return until;
    }
}
