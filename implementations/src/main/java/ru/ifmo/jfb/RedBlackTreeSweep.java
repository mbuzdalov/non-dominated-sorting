package ru.ifmo.jfb;

import ru.ifmo.util.RankQueryStructure;
import ru.ifmo.util.RedBlackRankQueryStructure;

public class RedBlackTreeSweep extends AbstractJFBSorting {
    public RedBlackTreeSweep(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    protected RankQueryStructure createStructure(int maximumPoints) {
        return new RedBlackRankQueryStructure(maximumPoints);
    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov sorting (tree sweep)";
    }
}
