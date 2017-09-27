package ru.ifmo.nds.tests;

import ru.ifmo.nds.JensenFortinBuzdalov;
import ru.ifmo.nds.NonDominatedSortingFactory;

public class JensenFortinBuzdalovRedBlackHybridENSParallelTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return JensenFortinBuzdalov.getRedBlackTreeSweepHybridENSImplementation(-1);
    }
}
