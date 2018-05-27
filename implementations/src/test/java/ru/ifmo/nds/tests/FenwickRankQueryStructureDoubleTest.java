package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.FenwickRankQueryStructureDouble;
import ru.ifmo.nds.util.RankQueryStructureDouble;

public class FenwickRankQueryStructureDoubleTest extends RankQueryStructureDoubleTestsBase {
    @Override
    protected RankQueryStructureDouble createStructure(int maximumPoints) {
        return new FenwickRankQueryStructureDouble(maximumPoints);
    }
}
