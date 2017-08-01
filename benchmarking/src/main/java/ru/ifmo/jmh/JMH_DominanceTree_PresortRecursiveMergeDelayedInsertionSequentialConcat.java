package ru.ifmo.jmh;

import ru.ifmo.DominanceTree;
import ru.ifmo.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_DominanceTree_PresortRecursiveMergeDelayedInsertionSequentialConcat extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DominanceTree.getPresortInsertion(true, DominanceTree.InsertionOption.DELAYED_INSERTION_SEQUENTIAL_CONCATENATION);
    }
}
