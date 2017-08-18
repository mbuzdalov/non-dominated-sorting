package ru.ifmo.nds.jmh;

import ru.ifmo.nds.NonDominatedSortingFactory;
import ru.ifmo.nds.SumitMishraDivideConquer;

@SuppressWarnings("unused")
public class JMH_SumitMishraDivideConquer_SS extends AbstractBenchmark {
    @Override
    public NonDominatedSortingFactory getFactory() {
        return SumitMishraDivideConquer.getSumitImplementation2016(false, false);
    }
}
