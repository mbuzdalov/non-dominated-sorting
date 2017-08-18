package ru.ifmo.nds.jmh;

import ru.ifmo.nds.DominanceTree;
import ru.ifmo.nds.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_DominanceTree_PresortSequentialMergeNoDelayedInsertion extends AbstractBenchmark {
    @Override
    public NonDominatedSortingFactory getFactory() {
        return DominanceTree.getPresortInsertion(false, DominanceTree.InsertionOption.NO_DELAYED_INSERTION);
    }
}
