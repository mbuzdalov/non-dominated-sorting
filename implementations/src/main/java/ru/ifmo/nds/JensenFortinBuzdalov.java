package ru.ifmo.nds;

import ru.ifmo.nds.jfb.FenwickSweep;
import ru.ifmo.nds.jfb.RedBlackTreeSweep;
import ru.ifmo.nds.jfb.RedBlackTreeSweepHybridLinearNDS;
import ru.ifmo.nds.jfb.RedBlackTreeSweepRankFilter;

public class JensenFortinBuzdalov {
    private JensenFortinBuzdalov() {}

    private static final NonDominatedSortingFactory RBTREE_SWEEP = RedBlackTreeSweep::new;
    private static final NonDominatedSortingFactory FENWICK_SWEEP = FenwickSweep::new;
    private static final NonDominatedSortingFactory RBTREE_HYBRID_LINEAR_FNDS = RedBlackTreeSweepHybridLinearNDS::new;
    private static final NonDominatedSortingFactory RBTREE_SWEEP_RANK_FILTER = RedBlackTreeSweepRankFilter::new;

    public static NonDominatedSortingFactory getRedBlackTreeSweepImplementation() {
        return RBTREE_SWEEP;
    }

    public static NonDominatedSortingFactory getFenwickSweepImplementation() {
        return FENWICK_SWEEP;
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridImplementation() {
        return RBTREE_HYBRID_LINEAR_FNDS;
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepWithRankFilterImplementation() {
        return RBTREE_SWEEP_RANK_FILTER;
    }
}
