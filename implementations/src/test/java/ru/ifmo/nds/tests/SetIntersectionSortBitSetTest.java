package ru.ifmo.nds.tests;

import ru.ifmo.nds.NonDominatedSortingFactory;
import ru.ifmo.nds.SetIntersectionSort;

public class SetIntersectionSortBitSetTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return SetIntersectionSort.getBitSetInstance();
    }
}
