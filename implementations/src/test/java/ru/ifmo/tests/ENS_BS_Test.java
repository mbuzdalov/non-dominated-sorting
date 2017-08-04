package ru.ifmo.tests;

import ru.ifmo.ENS;
import ru.ifmo.NonDominatedSortingFactory;

public class ENS_BS_Test extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return ENS.getENS_BS();
    }
}
