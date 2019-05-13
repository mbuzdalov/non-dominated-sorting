package ru.ifmo.nds.jfb.hybrid;

import ru.ifmo.nds.jfb.HybridAlgorithmWrapper;
import ru.ifmo.nds.jfb.JFBBase;
import ru.ifmo.nds.jfb.hybrid.tuning.Threshold;
import ru.ifmo.nds.jfb.hybrid.tuning.ThresholdFactory;
import ru.ifmo.nds.ndt.Split;
import ru.ifmo.nds.ndt.SplitBuilder;
import ru.ifmo.nds.ndt.TreeRankNode;

public final class NDT extends HybridAlgorithmWrapper {
    private final ThresholdFactory threshold3D;
    private final ThresholdFactory thresholdAll;
    private final int treeThreshold;

    public NDT(ThresholdFactory threshold3D, ThresholdFactory thresholdAll, int treeThreshold) {
        this.threshold3D = threshold3D;
        this.thresholdAll = thresholdAll;
        this.treeThreshold = treeThreshold;
    }

    @Override
    public boolean supportsMultipleThreads() {
        return true;
    }

    @Override
    public String getName() {
        return "NDT (threshold 3D = " + threshold3D.getDescription()
                + ", threshold all = " + thresholdAll.getDescription()
                + ", tree threshold = " + treeThreshold + ")";
    }

    @Override
    public HybridAlgorithmWrapper.Instance create(int[] ranks, int[] indices, double[][] points, double[][] transposedPoints) {
        return new Instance(ranks, indices, points, transposedPoints, threshold3D, thresholdAll, treeThreshold);
    }

    private static final ThreadLocal<TreeRankNode.RankEvaluationContext> contexts =
            ThreadLocal.withInitial(TreeRankNode.RankEvaluationContext::new);

    private static final class Instance extends HybridAlgorithmWrapper.Instance {
        private SplitBuilder splitBuilder;

        private double[][] points;
        private int[] indices;
        private int[] ranks;

        private double[][] localPoints;

        private final Threshold[] thresholds;
        private final int threshold;

        private Instance(int[] ranks, int[] indices, double[][] points, double[][] transposedPoints,
                         ThresholdFactory threshold3D, ThresholdFactory thresholdAll, int treeThreshold) {
            this.ranks = ranks;
            this.indices = indices;
            this.points = points;

            thresholds = new Threshold[8];
            for (int i = 0; i < thresholds.length; ++i) {
                thresholds[i] = (i == 0 ? threshold3D : thresholdAll).createThreshold();
            }

            this.threshold = treeThreshold;

            int maximumPoints = indices.length;
            int maximumDimension = transposedPoints.length;
            this.splitBuilder = new SplitBuilder(transposedPoints, maximumPoints, threshold);
            this.localPoints = new double[maximumPoints][maximumDimension];
        }

        private boolean notHookCondition(int size, int obj) {
            return obj == 1 || size >= thresholds[Math.min(obj - 2, 7)].getThreshold();
        }

        @Override
        public int helperAHook(int from, int until, int obj, int maximalMeaningfulRank) {
            if (notHookCondition(until - from, obj)) {
                return -1;
            }

            Split split = splitBuilder.result(from, until, indices, obj + 1);

            for (int i = from; i < until; ++i) {
                System.arraycopy(points[indices[i]], 1, localPoints[i], 1, obj);
            }

            TreeRankNode.RankEvaluationContext ctx = contexts.get();
            ctx.operations = 0;
            ctx.maxObj = obj;

            int minOverflow = until;
            TreeRankNode tree = threshold == 1 ? TreeRankNode.EMPTY_1 : TreeRankNode.EMPTY;
            for (int i = from; i < until; ++i) {
                int idx = indices[i];
                ctx.rank = ranks[idx];
                ctx.point = localPoints[i];
                tree.evaluateRank(ctx, split);
                ranks[idx] = ctx.rank;

                if (ranks[idx] <= maximalMeaningfulRank) {
                    tree = tree.add(localPoints[i], ranks[idx], split, threshold);
                } else if (minOverflow > i) {
                    minOverflow = i;
                }
            }
            return JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minOverflow, until);
        }

        @Override
        public int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom, int maximalMeaningfulRank) {
            if (notHookCondition(goodUntil - goodFrom + weakUntil - weakFrom, obj)) {
                return -1;
            }

            Split split = splitBuilder.result(goodFrom, goodUntil, indices, obj + 1);

            for (int good = goodFrom; good < goodUntil; ++good) {
                System.arraycopy(points[indices[good]], 1, localPoints[good], 1, obj);
            }
            for (int weak = weakFrom; weak < weakUntil; ++weak) {
                System.arraycopy(points[indices[weak]], 1, localPoints[weak], 1, obj);
            }

            TreeRankNode.RankEvaluationContext ctx = contexts.get();
            ctx.operations = 0;
            ctx.maxObj = obj;

            int minOverflow = weakUntil;
            TreeRankNode tree = threshold == 1 ? TreeRankNode.EMPTY_1 : TreeRankNode.EMPTY;
            for (int good = goodFrom, weak = weakFrom; weak < weakUntil; ++weak) {
                int wi = indices[weak];
                int gi;
                while (good < goodUntil && (gi = indices[good]) < wi) {
                    tree = tree.add(localPoints[good], ranks[gi], split, threshold);
                    ++good;
                }
                ctx.rank = ranks[wi];
                ctx.point = localPoints[weak];
                tree.evaluateRank(ctx, split);
                ranks[wi] = ctx.rank;
                if (minOverflow > weak && ranks[wi] > maximalMeaningfulRank) {
                    minOverflow = weak;
                }
            }
            return JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minOverflow, weakUntil);
        }
    }
}
