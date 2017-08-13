package ru.ifmo.nds.jmh;

import ru.ifmo.nds.NonDominatedSortingFactory;
import ru.ifmo.nds.SumitMishraDivideConquer;

@SuppressWarnings("unused")
public class JMH_SumitMishraDivideConquer_BSS extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return SumitMishraDivideConquer.getSumitImplementation2016(true, true);
    }
}
