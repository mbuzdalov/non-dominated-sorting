package ru.ifmo.nds.tests;

import ru.ifmo.nds.NonDominatedSortingFactory;
import ru.ifmo.nds.SumitMishraDivideConquer;

public class SumitMishraDivideConquer_SS_Test extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return SumitMishraDivideConquer.getSumitImplementation2016(false, false);
    }
}
