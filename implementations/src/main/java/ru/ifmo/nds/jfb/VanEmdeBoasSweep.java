package ru.ifmo.nds.jfb;

import ru.ifmo.nds.util.RankQueryStructure;
import ru.ifmo.nds.util.VanEmdeBoasRankQueryStructure;

public class VanEmdeBoasSweep extends AbstractJFBSorting {
    public VanEmdeBoasSweep(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension, 1);
    }

    @Override
    protected RankQueryStructure createStructure(int maximumPoints) {
        return new VanEmdeBoasRankQueryStructure(maximumPoints);
    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov sorting, " + getThreadDescription() + " (vEB sweep)";
    }
}
