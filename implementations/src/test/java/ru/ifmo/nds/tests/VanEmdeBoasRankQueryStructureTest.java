package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.RankQueryStructure;
import ru.ifmo.nds.util.VanEmdeBoasRankQueryStructure;

public class VanEmdeBoasRankQueryStructureTest extends RankQueryStructureTestsBase {
    @Override
    protected RankQueryStructure createStructure(int maximumPoints) {
        return new VanEmdeBoasRankQueryStructure(maximumPoints);
    }
}
