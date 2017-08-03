package ru.ifmo.jmh;

import ru.ifmo.DeductiveSort;
import ru.ifmo.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_DeductiveSort extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DeductiveSort.getInstance();
    }
}
