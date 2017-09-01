package ru.ifmo.nds.jfb;

import ru.ifmo.nds.util.RankQueryStructure;
import ru.ifmo.nds.util.RedBlackRankQueryStructure;

public class RedBlackTreeSweep extends AbstractJFBSorting {
    public RedBlackTreeSweep(int maximumPoints, int maximumDimension, boolean useRankFilter) {
        super(maximumPoints, maximumDimension, useRankFilter);
    }

    @Override
    protected RankQueryStructure createStructure(int maximumPoints) {
        return new RedBlackRankQueryStructure(maximumPoints);
    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov sorting (tree sweep" + (useRankFilter ? ", rank filter" : "") + ")";
    }
}
