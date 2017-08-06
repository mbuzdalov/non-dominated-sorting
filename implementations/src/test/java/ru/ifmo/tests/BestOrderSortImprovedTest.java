package ru.ifmo.tests;

import ru.ifmo.BestOrderSort;
import ru.ifmo.NonDominatedSortingFactory;

public class BestOrderSortImprovedTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return BestOrderSort.getImprovedImplementation();
    }
}
