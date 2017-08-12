package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.FenwickRankQueryStructure;
import ru.ifmo.nds.util.RankQueryStructure;

public class FenwickRankQueryStructureTest extends RankQueryStructureTestsBase {
    @Override
    protected RankQueryStructure createStructure(int maximumPoints) {
        return new FenwickRankQueryStructure(maximumPoints);
    }
}
