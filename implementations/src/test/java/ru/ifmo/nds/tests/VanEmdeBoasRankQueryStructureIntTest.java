package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.RankQueryStructureInt;
import ru.ifmo.nds.util.VanEmdeBoasRankQueryStructureInt;

public class VanEmdeBoasRankQueryStructureIntTest extends RankQueryStructureIntTestsBase {
    @Override
    protected RankQueryStructureInt createStructure(int maximumPoints) {
        return new VanEmdeBoasRankQueryStructureInt(maximumPoints);
    }
}
