package ru.ifmo.jmh;

import ru.ifmo.BestOrderSort;
import ru.ifmo.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_BestOrderSort_Improved extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return BestOrderSort.getImprovedImplementation();
    }
}
