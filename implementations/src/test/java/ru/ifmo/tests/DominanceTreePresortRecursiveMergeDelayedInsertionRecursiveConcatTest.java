package ru.ifmo.tests;

import ru.ifmo.DominanceTree;
import ru.ifmo.NonDominatedSortingFactory;

public class DominanceTreePresortRecursiveMergeDelayedInsertionRecursiveConcatTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DominanceTree.getPresortInsertion(true, DominanceTree.InsertionOption.DELAYED_INSERTION_RECURSIVE_CONCATENATION);
    }
}
