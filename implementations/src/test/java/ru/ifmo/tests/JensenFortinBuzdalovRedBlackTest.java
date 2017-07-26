package ru.ifmo.tests;

import ru.ifmo.JensenFortinBuzdalov;
import ru.ifmo.NonDominatedSortingFactory;

public class JensenFortinBuzdalovRedBlackTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return JensenFortinBuzdalov.getRedBlackTreeSweepImplementation();
    }
}
