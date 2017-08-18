package ru.ifmo.nds.jmh;

import ru.ifmo.nds.ENS;
import ru.ifmo.nds.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_ENS_SS extends AbstractBenchmark {
    @Override
    public NonDominatedSortingFactory getFactory() {
        return ENS.getENS_SS();
    }
}
