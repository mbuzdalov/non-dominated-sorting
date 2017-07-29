package ru.ifmo;

import ru.ifmo.domtree.Horstemeyer2008;

public class DominanceTree {
    private DominanceTree() {}

    private static final NonDominatedSortingFactory HORSTEMEYER_2008 = Horstemeyer2008::new;

    public static NonDominatedSortingFactory getHorstemeyer2008() {
        return HORSTEMEYER_2008;
    }
}
