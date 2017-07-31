package ru.ifmo.tests;

import ru.ifmo.DominanceTree;
import ru.ifmo.NonDominatedSortingFactory;

public class DominanceTreeNoDelayedInsertionSequentialMergeTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DominanceTree.getNoDelayedInsertion(false);
    }
}
