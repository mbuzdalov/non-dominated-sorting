package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.RankQueryStructureDouble;
import ru.ifmo.nds.util.RedBlackRankQueryStructure;

public class RedBlackRankQueryStructureTest extends RankQueryStructureDoubleTestsBase {
    @Override
    protected RankQueryStructureDouble createStructure(int maximumPoints) {
        return new RedBlackRankQueryStructure(maximumPoints);
    }
}
