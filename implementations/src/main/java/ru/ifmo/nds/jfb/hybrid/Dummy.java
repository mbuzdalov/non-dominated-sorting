package ru.ifmo.nds.jfb.hybrid;

import ru.ifmo.nds.jfb.HybridAlgorithmWrapper;

public final class Dummy extends HybridAlgorithmWrapper {
    private static final Dummy WRAPPER_INSTANCE = new Dummy();
    private Dummy() {}

    public static Dummy getWrapperInstance() {
        return WRAPPER_INSTANCE;
    }

    @Override
    public boolean supportsMultipleThreads() {
        return true;
    }

    @Override
    public String getName() {
        return "Dummy";
    }

    @Override
    public Instance create(int[] ranks, int[] indices, double[][] points, double[][] transposedPoints) {
        return INSTANCE;
    }

    private static final Instance INSTANCE = new Instance() {
        @Override
        public int helperAHook(int from, int until, int obj, int maximalMeaningfulRank) {
            return -1;
        }

        @Override
        public int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom, int maximalMeaningfulRank) {
            return -1;
        }
    };
}
