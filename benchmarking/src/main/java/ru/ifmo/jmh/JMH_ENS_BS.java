package ru.ifmo.jmh;

import ru.ifmo.ENS;
import ru.ifmo.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_ENS_BS extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return ENS.getENS_BS();
    }
}
