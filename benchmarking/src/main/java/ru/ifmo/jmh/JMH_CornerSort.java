package ru.ifmo.jmh;

import ru.ifmo.CornerSort;
import ru.ifmo.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_CornerSort extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return CornerSort.getInstance();
    }
}
