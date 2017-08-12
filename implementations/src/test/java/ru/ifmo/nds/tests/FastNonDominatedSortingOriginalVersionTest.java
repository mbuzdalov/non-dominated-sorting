package ru.ifmo.nds.tests;

import ru.ifmo.nds.FastNonDominatedSorting;
import ru.ifmo.nds.NonDominatedSortingFactory;

public class FastNonDominatedSortingOriginalVersionTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return FastNonDominatedSorting.getOriginalVersion();
    }
}
