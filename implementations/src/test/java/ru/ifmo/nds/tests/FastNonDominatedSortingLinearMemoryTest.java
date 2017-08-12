package ru.ifmo.nds.tests;

import ru.ifmo.nds.FastNonDominatedSorting;
import ru.ifmo.nds.NonDominatedSortingFactory;

public class FastNonDominatedSortingLinearMemoryTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return FastNonDominatedSorting.getLinearMemoryImplementation();
    }
}
