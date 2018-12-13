package ru.ifmo.nds.tests;

import ru.ifmo.nds.FilterSort;
import ru.ifmo.nds.NonDominatedSortingFactory;

public class FilterSortTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return FilterSort.getInstance();
    }
}
