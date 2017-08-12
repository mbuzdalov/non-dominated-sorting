package ru.ifmo.nds.jmh;

import ru.ifmo.nds.BestOrderSort;
import ru.ifmo.nds.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_BestOrderSort_Proteek extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return BestOrderSort.getProteekImplementation();
    }
}
