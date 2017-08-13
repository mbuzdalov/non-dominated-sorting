package ru.ifmo.nds.jmh;

import ru.ifmo.nds.NonDominatedSortingFactory;
import ru.ifmo.nds.SumitMishraDivideConquer;

@SuppressWarnings("unused")
public class JMH_SumitMishraDivideConquer_BS extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return SumitMishraDivideConquer.getSumitImplementation2016(true, false);
    }
}
