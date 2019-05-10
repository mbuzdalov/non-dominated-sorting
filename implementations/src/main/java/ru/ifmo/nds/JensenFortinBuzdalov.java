package ru.ifmo.nds;

import ru.ifmo.nds.jfb.*;
import ru.ifmo.nds.jfb.hybrid.Dummy;
import ru.ifmo.nds.jfb.hybrid.ENS;
import ru.ifmo.nds.jfb.hybrid.LinearNDS;
import ru.ifmo.nds.jfb.hybrid.NDT;
import ru.ifmo.nds.jfb.hybrid.tuning.ConstantThresholdFactory;
import ru.ifmo.nds.jfb.hybrid.tuning.DynamicThresholdFactory;
import ru.ifmo.nds.jfb.hybrid.tuning.ThresholdFactory;
import ru.ifmo.nds.util.FenwickRankQueryStructureDouble;
import ru.ifmo.nds.util.RedBlackRankQueryStructure;
import ru.ifmo.nds.util.VanEmdeBoasRankQueryStructureInt;

public final class JensenFortinBuzdalov {
    private JensenFortinBuzdalov() {}

    private static final ThresholdFactory CONSTANT_100 = new ConstantThresholdFactory(100);
    private static final ThresholdFactory CONSTANT_200 = new ConstantThresholdFactory(200);

    private static final ThresholdFactory DYNAMIC_100 = new DynamicThresholdFactory(100);
    private static final ThresholdFactory DYNAMIC_200 = new DynamicThresholdFactory(200);

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
        return (p, d) -> new JFBInt(new VanEmdeBoasRankQueryStructureInt(p), d, 1, new ENS(CONSTANT_100, CONSTANT_200));
    }

    public static NonDominatedSortingFactory getVanEmdeBoasHybridNDTImplementation(int threshold) {
        return (p, d) -> new JFBInt(new VanEmdeBoasRankQueryStructureInt(p), d, 1, new NDT(100, 20000, threshold));
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridFNDSImplementation(int allowedThreads) {
        return (p, d) -> new JFBDouble(new RedBlackRankQueryStructure(p), d, allowedThreads, LinearNDS.getWrapperInstance());
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridENSImplementation(int allowedThreads) {
        return (p, d) -> new JFBDouble(new RedBlackRankQueryStructure(p), d, allowedThreads, new ENS(CONSTANT_100, CONSTANT_200));
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridENSImplementationWithTuning(int allowedThreads) {
        return (p, d) -> new JFBDouble(new RedBlackRankQueryStructure(p), d, allowedThreads, new ENS(DYNAMIC_100, DYNAMIC_200));
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridNDTImplementation(int threshold, int allowedThreads) {
        return (p, d) -> new JFBDouble(new RedBlackRankQueryStructure(p), d, allowedThreads, new NDT(100, 20000, threshold));
    }
}
