package ru.ifmo.jfb;

import ru.ifmo.util.FenwickRankQueryStructure;
import ru.ifmo.util.RankQueryStructure;

public class FenwickSweep extends AbstractJFBSorting {
    public FenwickSweep(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    protected RankQueryStructure createStructure(int maximumPoints) {
        return new FenwickRankQueryStructure(maximumPoints);
    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov sorting (Fenwick sweep)";
    }

}
