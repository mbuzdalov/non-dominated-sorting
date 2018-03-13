package ru.ifmo.nds.tests;

import ru.ifmo.nds.BestOrderSort;
import ru.ifmo.nds.NonDominatedSortingFactory;

public class BestOrderSortImprovedReverseTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return BestOrderSort.getImprovedReverseImplementation();
    }
}
