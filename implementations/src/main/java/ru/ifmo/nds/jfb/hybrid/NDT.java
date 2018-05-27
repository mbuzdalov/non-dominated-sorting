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
        return false;
    }

    @Override
    public String getName() {
        return "NDT (threshold 3D = " + threshold3D + ", threshold all = " + thresholdAll + ", tree threshold = " + treeThreshold + ")";
    }

    @Override
    public HybridAlgorithmWrapper.Instance create(JFBBase.StateAccessor accessor) {
        return new Instance(accessor, threshold3D, thresholdAll, treeThreshold);
    }

    private static final class Instance extends HybridAlgorithmWrapper.Instance {
        private final JFBBase.StateAccessor accessor;
        private SplitBuilder splitBuilder;
        private TreeRankNode tree;

        private double[][] points;
        private double[][] transposedPoints;
        private int[] indices;
        private int[] ranks;

        private double[][] localPoints;

        private final int threshold3D;
        private final int thresholdAll;
        private final int threshold;

        private Instance(JFBBase.StateAccessor accessor, int threshold3D, int thresholdAll, int treeThreshold) {
            this.accessor = accessor;

            this.points = accessor.getPoints();
            this.indices = accessor.getIndices();
            this.ranks = accessor.getRanks();
            this.transposedPoints = accessor.getTransposedPoints();

            this.threshold3D = threshold3D;
            this.thresholdAll = thresholdAll;
            this.threshold = treeThreshold;

            int maximumPoints = indices.length;
            int maximumDimension = transposedPoints.length;
            this.splitBuilder = new SplitBuilder(maximumPoints);
            this.tree = TreeRankNode.EMPTY;
            this.localPoints = new double[maximumPoints][maximumDimension];
        }

        @Override
        public boolean helperAHookCondition(int size, int obj) {
            switch (obj) {
                case 1: return false;
                case 2: return size < threshold3D;
                default: return size < thresholdAll;
            }
        }

        @Override
        public boolean helperBHookCondition(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
            return helperAHookCondition(goodUntil - goodFrom + weakUntil - weakFrom, obj);
        }

        @Override
        public int helperAHook(int from, int until, int obj) {
            int M = obj + 1;
            Split split = splitBuilder.result(transposedPoints, from, until, indices, M, threshold);

            for (int i = from; i < until; ++i) {
                System.arraycopy(points[indices[i]], 0, localPoints[i], 0, M);
            }

            int maximalMeaningfulRank = accessor.getMaximalMeaningfulRank();
            int minOverflow = until;
            tree = TreeRankNode.EMPTY;
            for (int i = from; i < until; ++i) {
                int idx = indices[i];
                ranks[idx] = tree.evaluateRank(localPoints[i], ranks[idx], split, M);

                if (ranks[idx] <= maximalMeaningfulRank) {
                    tree = tree.add(localPoints[i], ranks[idx], split, threshold);
                } else if (minOverflow > i) {
                    minOverflow = i;
                }
            }
            tree = null;
            return accessor.kickOutOverflowedRanks(minOverflow, until);
        }

        @Override
        public int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom) {
            int M = obj + 1;
            Split split = splitBuilder.result(transposedPoints, goodFrom, goodUntil, indices, M, threshold);

            for (int good = goodFrom; good < goodUntil; ++good) {
                System.arraycopy(points[indices[good]], 0, localPoints[good], 0, M);
            }
            for (int weak = weakFrom; weak < weakUntil; ++weak) {
                System.arraycopy(points[indices[weak]], 0, localPoints[weak], 0, M);
            }

            int maximalMeaningfulRank = accessor.getMaximalMeaningfulRank();
            int minOverflow = weakUntil;
            tree = TreeRankNode.EMPTY;
            for (int good = goodFrom, weak = weakFrom; weak < weakUntil; ++weak) {
                int wi = indices[weak];
                int gi;
                while (good < goodUntil && (gi = indices[good]) < wi) {
                    tree = tree.add(localPoints[good], ranks[gi], split, threshold);
                    ++good;
                }
                ranks[wi] = tree.evaluateRank(localPoints[weak], ranks[wi], split, M);
                if (minOverflow > weak && ranks[wi] > maximalMeaningfulRank) {
                    minOverflow = weak;
                }
            }
            tree = null;
            return accessor.kickOutOverflowedRanks(minOverflow, weakUntil);
        }
    }
}
