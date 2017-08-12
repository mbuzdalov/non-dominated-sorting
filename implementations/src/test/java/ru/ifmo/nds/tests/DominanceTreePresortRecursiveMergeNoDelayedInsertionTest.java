package ru.ifmo.nds.tests;

import ru.ifmo.nds.DominanceTree;
import ru.ifmo.nds.NonDominatedSortingFactory;

public class DominanceTreePresortRecursiveMergeNoDelayedInsertionTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DominanceTree.getPresortInsertion(true, DominanceTree.InsertionOption.NO_DELAYED_INSERTION);
    }
}
