package ru.ifmo.tests;

import ru.ifmo.DominanceTree;
import ru.ifmo.NonDominatedSortingFactory;

public class DominanceTreeNoDelayedInsertionRecursiveMergeTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DominanceTree.getNoDelayedInsertion(true);
    }
}
