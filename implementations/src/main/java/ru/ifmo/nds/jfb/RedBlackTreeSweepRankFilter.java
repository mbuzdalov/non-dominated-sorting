package ru.ifmo.nds.jfb;

import ru.ifmo.nds.util.RankQueryStructure;
import ru.ifmo.nds.util.RedBlackRankQueryStructure;

public class RedBlackTreeSweepRankFilter extends AbstractJFBSorting {
    private int lastMaxRankResult = -1;
    private int[] buffer;

    public RedBlackTreeSweepRankFilter(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        buffer = new int[maximumPoints];
    }

    @Override
    protected RankQueryStructure createStructure(int maximumPoints) {
        return new RedBlackRankQueryStructure(maximumPoints);
    }

    @Override
    protected void closeImpl() throws Exception {
        super.closeImpl();
        buffer = null;
    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov sorting (tree sweep, rank filter)";
    }

    private int getMaxRank(int from, int until) {
        int rv = -1;
        for (int i = from; i < until; ++i) {
            rv = Math.max(rv, ranks[indices[i]]);
        }
        lastMaxRankResult = rv;
        return rv;
    }

    @Override
    protected boolean helperBHookCondition(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        int maxGoodRank = getMaxRank(goodFrom, goodUntil);
        for (int i = weakFrom; i < weakUntil; ++i) {
            if (ranks[indices[i]] > maxGoodRank) {
                return true;
            }
        }
        return false;
    }

    @Override
    protected int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        int maxGoodRank = lastMaxRankResult;
        int lastFiltered = weakFrom;
        int missing = 0;
        for (int i = weakFrom; i < weakUntil; ++i) {
            if (ranks[indices[i]] > maxGoodRank) {
                buffer[missing++] = indices[i];
            } else {
                indices[lastFiltered++] = indices[i];
            }
        }
        if (missing != weakUntil - lastFiltered) {
            throw new AssertionError();
        }
        System.arraycopy(buffer, 0, indices, lastFiltered, missing);

        int rv = helperB(goodFrom, goodUntil, weakFrom, lastFiltered, obj);
        mergeTwo(weakFrom, rv, lastFiltered, weakUntil);
        return rv + missing;
    }
}
