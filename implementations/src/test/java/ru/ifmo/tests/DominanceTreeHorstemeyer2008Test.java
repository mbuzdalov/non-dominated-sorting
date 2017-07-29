package ru.ifmo.tests;

import org.junit.Ignore;
import ru.ifmo.DominanceTree;
import ru.ifmo.NonDominatedSortingFactory;

@Ignore
public class DominanceTreeHorstemeyer2008Test extends CorrectnessTestsBase {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return DominanceTree.getHorstemeyer2008();
    }
}
