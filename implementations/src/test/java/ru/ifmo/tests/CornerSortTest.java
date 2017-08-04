package ru.ifmo.tests;

import ru.ifmo.CornerSort;
import ru.ifmo.NonDominatedSortingFactory;

public class CornerSortTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return CornerSort.getInstance();
    }
}
