package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.RankQueryStructure;
import ru.ifmo.nds.util.RedBlackRankQueryStructure;

public class RedBlackRankQueryStructureTest extends RankQueryStructureTestsBase {
    @Override
    protected RankQueryStructure createStructure(int maximumPoints) {
        return new RedBlackRankQueryStructure(maximumPoints);
    }
}
