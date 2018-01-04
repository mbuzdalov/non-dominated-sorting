package ru.ifmo.nds.tests;

import ru.ifmo.nds.ENS;
import ru.ifmo.nds.NonDominatedSortingFactory;

public class ENS_NDT_Arrays_Test extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return ENS.getENS_NDT_Arrays();
    }
}
