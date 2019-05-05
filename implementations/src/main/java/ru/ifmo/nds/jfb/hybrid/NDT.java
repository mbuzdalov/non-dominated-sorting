package ru.ifmo.nds.jfb.hybrid;

import ru.ifmo.nds.jfb.HybridAlgorithmWrapper;
import ru.ifmo.nds.jfb.JFBBase;
import ru.ifmo.nds.ndt.Split;
import ru.ifmo.nds.ndt.SplitBuilder;
import ru.ifmo.nds.ndt.TreeRankNode;

public final class NDT extends HybridAlgorithmWrapper {
    private final int threshold3D;
    private final int thresholdAll;
    private final int treeThreshold;

    public NDT(int threshold3D, int thresholdAll, int treeThreshold) {
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
        return "NDT (threshold 3D = " + threshold3D + ", threshold all = " + thresholdAll + ", tree threshold = " + treeThreshold + ")";
    }

    @Override
    public HybridAlgorithmWrapper.Instance create(int[] ranks, int[] indices, double[][] points, double[][] transposedPoints) {
        return new Instance(ranks, indices, points, transposedPoints, threshold3D, thresholdAll, treeThreshold);
    }

    private static final class Instance extends HybridAlgorithmWrapper.Instance {
        private SplitBuilder splitBuilder;

        private double[][] points;
        private int[] indices;
        private int[] ranks;

        private double[][] localPoints;

        private final int threshold3D;
        private final int thresholdAll;
        private final int threshold;

        private Instance(int[] ranks, int[] indices, double[][] points, double[][] transposedPoints, int threshold3D, int thresholdAll, int treeThreshold) {
            this.ranks = ranks;
            this.indices = indices;
            this.points = points;

            this.threshold3D = threshold3D;
            this.thresholdAll = thresholdAll;
            this.threshold = treeThreshold;

            int maximumPoints = indices.length;
            int maximumDimension = transposedPoints.length;
            this.splitBuilder = new SplitBuilder(transposedPoints, maximumPoints, threshold);
            this.localPoints = new double[maximumPoints][maximumDimension];
        }

        private boolean notHookCondition(int size, int obj) {
            switch (obj) {
                case 1: return true;
                case 2: return size >= threshold3D;
                default: return size >= thresholdAll;
            }
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

            int minOverflow = until;
            TreeRankNode tree = threshold == 1 ? TreeRankNode.EMPTY_1 : TreeRankNode.EMPTY;
            for (int i = from; i < until; ++i) {
                int idx = indices[i];
                ranks[idx] = tree.evaluateRank(localPoints[i], ranks[idx], split, obj);

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

            int minOverflow = weakUntil;
            TreeRankNode tree = threshold == 1 ? TreeRankNode.EMPTY_1 : TreeRankNode.EMPTY;
            for (int good = goodFrom, weak = weakFrom; weak < weakUntil; ++weak) {
                int wi = indices[weak];
                int gi;
                while (good < goodUntil && (gi = indices[good]) < wi) {
                    tree = tree.add(localPoints[good], ranks[gi], split, threshold);
                    ++good;
                }
                ranks[wi] = tree.evaluateRank(localPoints[weak], ranks[wi], split, obj);
                if (minOverflow > weak && ranks[wi] > maximalMeaningfulRank) {
                    minOverflow = weak;
                }
            }
            return JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minOverflow, weakUntil);
        }
    }
}
