package ru.ifmo.nds.jmh;

import ru.ifmo.nds.DominanceTree;
import ru.ifmo.nds.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_DominanceTree_NoPresortSequentialMerge extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DominanceTree.getNoPresortInsertion(false);
    }
}
