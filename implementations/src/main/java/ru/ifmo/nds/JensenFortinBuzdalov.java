package ru.ifmo.nds;

import ru.ifmo.nds.jfb.FenwickSweep;
import ru.ifmo.nds.jfb.RedBlackTreeSweep;
import ru.ifmo.nds.jfb.RedBlackTreeSweepHybridLinearNDS;

public class JensenFortinBuzdalov {
    private JensenFortinBuzdalov() {}

    private static final NonDominatedSortingFactory FENWICK_SWEEP = FenwickSweep::new;

    public static NonDominatedSortingFactory getRedBlackTreeSweepImplementation(boolean useRankFilter) {
        return (n, d) -> new RedBlackTreeSweep(n, d, useRankFilter);
    }

    public static NonDominatedSortingFactory getFenwickSweepImplementation() {
        return FENWICK_SWEEP;
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridImplementation(boolean useRankFilter) {
        return (n, d) -> new RedBlackTreeSweepHybridLinearNDS(n, d, useRankFilter);
    }
}
