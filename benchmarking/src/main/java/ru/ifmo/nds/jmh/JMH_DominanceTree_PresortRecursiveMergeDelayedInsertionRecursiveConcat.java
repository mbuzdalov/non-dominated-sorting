package ru.ifmo.nds.jmh;

import ru.ifmo.nds.DominanceTree;
import ru.ifmo.nds.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_DominanceTree_PresortRecursiveMergeDelayedInsertionRecursiveConcat extends AbstractBenchmark {
    @Override
    public NonDominatedSortingFactory getFactory() {
        return DominanceTree.getPresortInsertion(true, DominanceTree.InsertionOption.DELAYED_INSERTION_RECURSIVE_CONCATENATION);
    }
}
