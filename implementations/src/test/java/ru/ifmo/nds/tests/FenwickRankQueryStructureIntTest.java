package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.FenwickRankQueryStructureInt;
import ru.ifmo.nds.util.RankQueryStructureInt;

public class FenwickRankQueryStructureIntTest extends RankQueryStructureIntTestsBase {
    @Override
    protected RankQueryStructureInt createStructure(int maximumPoints) {
        return new FenwickRankQueryStructureInt(maximumPoints);
    }
}
