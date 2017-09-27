package ru.ifmo.nds;

import ru.ifmo.nds.jfb.FenwickSweep;
import ru.ifmo.nds.jfb.RedBlackTreeSweep;
import ru.ifmo.nds.jfb.RedBlackTreeSweepHybridENS;
import ru.ifmo.nds.jfb.RedBlackTreeSweepHybridLinearNDS;

public class JensenFortinBuzdalov {
    private JensenFortinBuzdalov() {}

    public static NonDominatedSortingFactory getRedBlackTreeSweepImplementation(int allowedThreads) {
        return (p, d) -> new RedBlackTreeSweep(p, d, allowedThreads);
    }

    public static NonDominatedSortingFactory getFenwickSweepImplementation(int allowedThreads) {
        return (p, d) -> new FenwickSweep(p, d, allowedThreads);
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridFNDSImplementation(int allowedThreads) {
        return (p, d) -> new RedBlackTreeSweepHybridLinearNDS(p, d, allowedThreads);
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridENSImplementation(int allowedThreads) {
        return (p, d) -> new RedBlackTreeSweepHybridENS(p, d, allowedThreads);
    }
}
