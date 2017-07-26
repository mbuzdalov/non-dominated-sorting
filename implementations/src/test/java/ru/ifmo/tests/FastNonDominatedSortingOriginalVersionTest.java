package ru.ifmo.tests;

import ru.ifmo.FastNonDominatedSorting;
import ru.ifmo.NonDominatedSortingFactory;

public class FastNonDominatedSortingOriginalVersionTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return FastNonDominatedSorting.getOriginalVersion();
    }
}
