package ru.ifmo.tests;

import ru.ifmo.util.RankQueryStructure;
import ru.ifmo.util.RedBlackRankQueryStructure;

public class RedBlackRankQueryStructureTest extends RankQueryStructureTestsBase {
    @Override
    protected RankQueryStructure createStructure(int maximumPoints) {
        return new RedBlackRankQueryStructure(maximumPoints);
    }
}
