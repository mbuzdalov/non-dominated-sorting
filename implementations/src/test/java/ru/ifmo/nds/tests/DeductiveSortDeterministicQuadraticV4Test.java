package ru.ifmo.nds.tests;

import ru.ifmo.nds.DeductiveSort;
import ru.ifmo.nds.NonDominatedSortingFactory;

public class DeductiveSortDeterministicQuadraticV4Test extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DeductiveSort.getDeterministicQuadraticImplementationV4();
    }
}
