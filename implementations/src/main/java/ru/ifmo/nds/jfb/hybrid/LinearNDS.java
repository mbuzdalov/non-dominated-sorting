package ru.ifmo.nds.jfb.hybrid;

import ru.ifmo.nds.jfb.HybridAlgorithmWrapper;
import ru.ifmo.nds.jfb.JFBBase;

public final class LinearNDS extends HybridAlgorithmWrapper {
    private static final int THRESHOLD_3D = 50;
    private static final int THRESHOLD_ALL = 100;

    private static final LinearNDS WRAPPER_INSTANCE = new LinearNDS();
    private LinearNDS() {}

    public static LinearNDS getWrapperInstance() {
        return WRAPPER_INSTANCE;
    }

    @Override
    public boolean supportsMultipleThreads() {
        return true;
    }

    @Override
    public String getName() {
        return "LinearNDS";
    }

    @Override
    public HybridAlgorithmWrapper.Instance create(int[] ranks, int[] indices, double[][] points, double[][] transposedPoints) {
        return new Instance(ranks, indices, points);
    }

    private static final class Instance extends HybridAlgorithmWrapper.Instance {
        private final int[] ranks;
        private final int[] indices;
        private final double[][] points;

        private Instance(int[] ranks, int[] indices, double[][] points) {
            this.ranks = ranks;
            this.indices = indices;
            this.points = points;
        }

        private boolean notHookCondition(int size, int obj) {
            switch (obj) {
                case 1: return true;
                case 2: return size >= THRESHOLD_3D;
                default: return size >= THRESHOLD_ALL;
            }
        }

        @Override
        public int helperAHook(int from, int until, int obj, int maximalMeaningfulRank) {
            if (notHookCondition(until - from, obj)) {
                return -from - 1;
            }
            for (int left = from; left < until; ++left) {
                until = JFBBase.updateByPoint(ranks, indices, points, maximalMeaningfulRank, indices[left], left + 1, until, obj);
            }
            return until;
        }

        @Override
        public int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom, int maximalMeaningfulRank) {
            if (notHookCondition(goodUntil - goodFrom + weakUntil - weakFrom, obj)) {
                return -weakFrom - 1;
            }
            for (int good = goodFrom, weakMin = weakFrom; good < goodUntil; ++good) {
                int goodIndex = indices[good];
                while (weakMin < weakUntil && indices[weakMin] < goodIndex) {
                    ++weakMin;
                }
                weakUntil = JFBBase.updateByPoint(ranks, indices, points, maximalMeaningfulRank, goodIndex, weakMin, weakUntil, obj);
            }
            return weakUntil;
        }
    }
}
