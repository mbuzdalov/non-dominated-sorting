package ru.ifmo;

import ru.ifmo.jfb.FenwickSweep;
import ru.ifmo.jfb.RedBlackTreeSweep;

public class JensenFortinBuzdalov {
    private JensenFortinBuzdalov() {}

    private static final NonDominatedSortingFactory RBTREE_SWEEP = RedBlackTreeSweep::new;
    private static final NonDominatedSortingFactory FENWICK_SWEEP = FenwickSweep::new;

    public static NonDominatedSortingFactory getRedBlackTreeSweepImplementation() {
        return RBTREE_SWEEP;
    }

    public static NonDominatedSortingFactory getFenwickSweepImplementation() {
        return FENWICK_SWEEP;
    }
}
