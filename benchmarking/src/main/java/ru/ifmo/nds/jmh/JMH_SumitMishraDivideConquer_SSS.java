package ru.ifmo.nds.jmh;

import ru.ifmo.nds.NonDominatedSortingFactory;
import ru.ifmo.nds.SumitMishraDivideConquer;

@SuppressWarnings("unused")
public class JMH_SumitMishraDivideConquer_SSS extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return SumitMishraDivideConquer.getSumitImplementation2016(false, true);
    }
}
