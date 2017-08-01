package ru.ifmo.tests;

import ru.ifmo.DominanceTree;
import ru.ifmo.NonDominatedSortingFactory;

public class DominanceTreePresortSequentialMergeDelayedInsertionRecursiveConcatTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DominanceTree.getPresortInsertion(false, DominanceTree.InsertionOption.DELAYED_INSERTION_RECURSIVE_CONCATENATION);
    }
}
