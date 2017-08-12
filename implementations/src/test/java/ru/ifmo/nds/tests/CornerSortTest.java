package ru.ifmo.nds.tests;

import ru.ifmo.nds.CornerSort;
import ru.ifmo.nds.NonDominatedSortingFactory;

public class CornerSortTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return CornerSort.getInstance();
    }
}
