package ru.ifmo.tests;

import ru.ifmo.DeductiveSort;
import ru.ifmo.NonDominatedSortingFactory;

public class DeductiveSortTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DeductiveSort.getInstance();
    }
}
