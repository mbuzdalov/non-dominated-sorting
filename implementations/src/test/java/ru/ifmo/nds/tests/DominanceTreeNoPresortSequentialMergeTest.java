package ru.ifmo.nds.tests;

import ru.ifmo.nds.DominanceTree;
import ru.ifmo.nds.NonDominatedSortingFactory;

public class DominanceTreeNoPresortSequentialMergeTest extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DominanceTree.getNoPresortInsertion(false);
    }
}
