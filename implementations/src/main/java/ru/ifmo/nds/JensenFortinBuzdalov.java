package ru.ifmo.nds;

import ru.ifmo.nds.jfb.*;
import ru.ifmo.nds.jfb.hybrid.Dummy;
import ru.ifmo.nds.jfb.hybrid.ENS;
import ru.ifmo.nds.jfb.hybrid.LinearNDS;
import ru.ifmo.nds.jfb.hybrid.NDT;
import ru.ifmo.nds.util.FenwickRankQueryStructureDouble;
import ru.ifmo.nds.util.RedBlackRankQueryStructure;
import ru.ifmo.nds.util.VanEmdeBoasRankQueryStructureInt;

public class JensenFortinBuzdalov {
    private JensenFortinBuzdalov() {}

    public static NonDominatedSortingFactory getRedBlackTreeSweepImplementation(int allowedThreads) {
        return (p, d) -> new JFBDouble(new RedBlackRankQueryStructure(p), d, allowedThreads, Dummy.getWrapperInstance());
    }

    public static NonDominatedSortingFactory getFenwickSweepImplementation(int allowedThreads) {
        return (p, d) -> new JFBDouble(new FenwickRankQueryStructureDouble(p), d, allowedThreads, Dummy.getWrapperInstance());
    }

    public static NonDominatedSortingFactory getVanEmdeBoasImplementation() {
        return (p, d) -> new JFBInt(new VanEmdeBoasRankQueryStructureInt(p), d, 1, Dummy.getWrapperInstance());
    }

    public static NonDominatedSortingFactory getVanEmdeBoasHybridENSImplementation() {
        return (p, d) -> new JFBInt(new VanEmdeBoasRankQueryStructureInt(p), d, 1, new ENS(100, 200));
    }

    public static NonDominatedSortingFactory getVanEmdeBoasHybridNDTImplementation(int threshold) {
        return (p, d) -> new JFBInt(new VanEmdeBoasRankQueryStructureInt(p), d, 1, new NDT(100, 20000, threshold));
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridFNDSImplementation(int allowedThreads) {
        return (p, d) -> new JFBDouble(new RedBlackRankQueryStructure(p), d, allowedThreads, LinearNDS.getWrapperInstance());
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridENSImplementation(int allowedThreads) {
        return (p, d) -> new JFBDouble(new RedBlackRankQueryStructure(p), d, allowedThreads, new ENS(100, 200));
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridNDTImplementation(int threshold) {
        return (p, d) -> new JFBDouble(new RedBlackRankQueryStructure(p), d, 1, new NDT(100, 20000, threshold));
    }
}
