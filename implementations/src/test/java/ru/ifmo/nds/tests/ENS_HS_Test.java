package ru.ifmo.nds.tests;

import ru.ifmo.nds.ENS;
import ru.ifmo.nds.NonDominatedSortingFactory;

public class ENS_HS_Test extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return ENS.getENS_HS();
    }
}
