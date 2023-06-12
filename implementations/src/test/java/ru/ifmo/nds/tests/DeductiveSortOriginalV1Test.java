package ru.ifmo.nds.tests;

import ru.ifmo.nds.DeductiveSort;
import ru.ifmo.nds.NonDominatedSortingFactory;

public class DeductiveSortOriginalV1Test extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DeductiveSort.getOriginalImplementationV1();
    }
}
