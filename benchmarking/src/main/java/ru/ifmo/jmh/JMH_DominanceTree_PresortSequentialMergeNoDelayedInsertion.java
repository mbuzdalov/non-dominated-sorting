package ru.ifmo.jmh;

import ru.ifmo.DominanceTree;
import ru.ifmo.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_DominanceTree_PresortSequentialMergeNoDelayedInsertion extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DominanceTree.getPresortInsertion(false, DominanceTree.InsertionOption.NO_DELAYED_INSERTION);
    }
}
