package ru.ifmo.nds.jmh;

import ru.ifmo.nds.CornerSort;
import ru.ifmo.nds.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_CornerSort extends AbstractBenchmark {
    @Override
    public NonDominatedSortingFactory getFactory() {
        return CornerSort.getInstance();
    }
}
