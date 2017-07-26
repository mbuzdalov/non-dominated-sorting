package ru.ifmo.tests;

import ru.ifmo.util.FenwickRankQueryStructure;
import ru.ifmo.util.RankQueryStructure;

public class FenwickRankQueryStructureTest extends RankQueryStructureTestsBase {
    @Override
    protected RankQueryStructure createStructure(int maximumPoints) {
        return new FenwickRankQueryStructure(maximumPoints);
    }
}
