package ru.ifmo.nds;

import ru.ifmo.nds.jfb.*;
import ru.ifmo.nds.jfb.hybrid.Dummy;
import ru.ifmo.nds.jfb.hybrid.ENS;
import ru.ifmo.nds.jfb.hybrid.LinearNDS;
import ru.ifmo.nds.jfb.hybrid.NDT;
import ru.ifmo.nds.jfb.hybrid.tuning.*;
import ru.ifmo.nds.util.FenwickRankQueryStructureDouble;
import ru.ifmo.nds.util.RedBlackRankQueryStructure;
import ru.ifmo.nds.util.VanEmdeBoasRankQueryStructureInt;

public final class JensenFortinBuzdalov {
    private JensenFortinBuzdalov() {}

    private static final ThresholdFactory CONSTANT_100 = new ConstantThresholdFactory(100);
    private static final ThresholdFactory CONSTANT_200 = new ConstantThresholdFactory(200);
    private static final ThresholdFactory CONSTANT_20000 = new ConstantThresholdFactory(20000);

    private static final ThresholdFactory DYNAMIC_100 = new DynamicThresholdFactory(100);
    private static final ThresholdFactory DYNAMIC_200 = new DynamicThresholdFactory(200);

    private static final ThresholdFactory DYNAMIC_100_ADJINC = new DynamicAdjustableIncreaseThresholdFactory(100);
    private static final ThresholdFactory DYNAMIC_200_ADJINC = new DynamicAdjustableIncreaseThresholdFactory(200);

    private static final ThresholdFactory DYNAMIC_100_ADJBOTH = new DynamicAdjustableBothThresholdFactory(100);
    private static final ThresholdFactory DYNAMIC_200_ADJBOTH = new DynamicAdjustableBothThresholdFactory(200);


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
        return (p, d) -> new JFBInt(new VanEmdeBoasRankQueryStructureInt(p), d, 1, new NDT(CONSTANT_100, CONSTANT_20000, threshold));
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

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridENSImplementationWithTuningAdjustableInc(int allowedThreads) {
        return (p, d) -> new JFBDouble(new RedBlackRankQueryStructure(p), d, allowedThreads, new ENS(DYNAMIC_100_ADJINC, DYNAMIC_200_ADJINC));
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridENSImplementationWithTuningAdjustableBoth(int allowedThreads) {
        return (p, d) -> new JFBDouble(new RedBlackRankQueryStructure(p), d, allowedThreads, new ENS(DYNAMIC_100_ADJBOTH, DYNAMIC_200_ADJBOTH));
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridNDTImplementation(int threshold, int allowedThreads) {
        return (p, d) -> new JFBDouble(new RedBlackRankQueryStructure(p), d, allowedThreads, new NDT(CONSTANT_100, CONSTANT_20000, threshold));
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridNDTImplementationWithTuning(int threshold, int allowedThreads) {
        return (p, d) -> new JFBDouble(new RedBlackRankQueryStructure(p), d, allowedThreads, new NDT(DYNAMIC_100, DYNAMIC_200, threshold));
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridNDTImplementationWithTuningAdjustableInc(int threshold, int allowedThreads) {
        return (p, d) -> new JFBDouble(new RedBlackRankQueryStructure(p), d, allowedThreads, new NDT(DYNAMIC_100_ADJINC, DYNAMIC_200_ADJINC, threshold));
    }

    public static NonDominatedSortingFactory getRedBlackTreeSweepHybridNDTImplementationWithTuningAdjustableBoth(int threshold, int allowedThreads) {
        return (p, d) -> new JFBDouble(new RedBlackRankQueryStructure(p), d, allowedThreads, new NDT(DYNAMIC_100_ADJBOTH, DYNAMIC_200_ADJBOTH, threshold));
    }
}
