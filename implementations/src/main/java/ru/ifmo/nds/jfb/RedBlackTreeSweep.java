package ru.ifmo.nds.jfb;

import ru.ifmo.nds.util.RankQueryStructure;
import ru.ifmo.nds.util.RedBlackRankQueryStructure;

public class RedBlackTreeSweep extends AbstractJFBSorting {
    public RedBlackTreeSweep(int maximumPoints, int maximumDimension, int allowedThreads) {
        super(maximumPoints, maximumDimension, allowedThreads);
    }

    @Override
    protected RankQueryStructure createStructure(int maximumPoints) {
        return new RedBlackRankQueryStructure(maximumPoints);
    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov sorting, " + getThreadDescription() + " (tree sweep)";
    }
}
