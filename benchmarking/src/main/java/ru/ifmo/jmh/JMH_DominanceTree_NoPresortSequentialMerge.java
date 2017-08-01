package ru.ifmo.jmh;

import ru.ifmo.DominanceTree;
import ru.ifmo.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_DominanceTree_NoPresortSequentialMerge extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DominanceTree.getNoPresortInsertion(false);
    }
}
