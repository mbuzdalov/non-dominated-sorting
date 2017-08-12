package ru.ifmo.nds.tests;

import ru.ifmo.nds.DominanceTree;
import ru.ifmo.nds.NonDominatedSortingFactory;

public class DominanceTreePresortRecursiveMergeDelayedInsertionRecursiveConcatTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DominanceTree.getPresortInsertion(true, DominanceTree.InsertionOption.DELAYED_INSERTION_RECURSIVE_CONCATENATION);
    }
}
