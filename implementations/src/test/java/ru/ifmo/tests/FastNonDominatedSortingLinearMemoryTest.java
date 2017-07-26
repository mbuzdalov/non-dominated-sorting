package ru.ifmo.tests;

import ru.ifmo.FastNonDominatedSorting;
import ru.ifmo.NonDominatedSortingFactory;

public class FastNonDominatedSortingLinearMemoryTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return FastNonDominatedSorting.getLinearMemoryImplementation();
    }
}
