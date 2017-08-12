package ru.ifmo.nds.jmh;

import ru.ifmo.nds.DominanceTree;
import ru.ifmo.nds.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_DominanceTree_PresortSequentialMergeDelayedInsertionSequentialConcat extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DominanceTree.getPresortInsertion(false, DominanceTree.InsertionOption.DELAYED_INSERTION_SEQUENTIAL_CONCATENATION);
    }
}
