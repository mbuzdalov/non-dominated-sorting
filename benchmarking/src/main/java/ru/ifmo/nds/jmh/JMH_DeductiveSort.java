package ru.ifmo.nds.jmh;

import ru.ifmo.nds.DeductiveSort;
import ru.ifmo.nds.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_DeductiveSort extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DeductiveSort.getInstance();
    }
}
