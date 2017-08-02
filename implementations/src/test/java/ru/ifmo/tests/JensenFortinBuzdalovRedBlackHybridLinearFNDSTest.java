package ru.ifmo.tests;

import ru.ifmo.JensenFortinBuzdalov;
import ru.ifmo.NonDominatedSortingFactory;

public class JensenFortinBuzdalovRedBlackHybridLinearFNDSTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return JensenFortinBuzdalov.getRedBlackTreeSweepHybridImplementation();
    }
}
