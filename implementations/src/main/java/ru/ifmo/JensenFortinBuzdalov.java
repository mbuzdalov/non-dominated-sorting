package ru.ifmo;

import ru.ifmo.jfb.FenwickSweep;
import ru.ifmo.jfb.RedBlackTreeSweep;
import ru.ifmo.jfb.RedBlackTreeSweepHybridLinearNDS;

public class JensenFortinBuzdalov {
    private JensenFortinBuzdalov() {}

    private static final NonDominatedSortingFactory RBTREE_SWEEP = RedBlackTreeSweep::new;
    private static final NonDominatedSortingFactory FENWICK_SWEEP = FenwickSweep::new;
    private static final NonDominatedSortingFactory RBTREE_HYBRID_LINEAR_FNDS = RedBlackTreeSweepHybridLinearNDS::new;

    public static NonDominatedSortingFactory getRedBlackTreeSweepImplementation() {
        return RBTREE_SWEEP;
    }

    public static NonDominatedSortingFactory getFenwickSweepImplementation() {
        return FENWICK_SWEEP;
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridImplementation() {
        return RBTREE_HYBRID_LINEAR_FNDS;
    }
}
