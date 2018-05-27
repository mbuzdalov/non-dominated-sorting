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
    public HybridAlgorithmWrapper.Instance create(JFBBase.StateAccessor accessor) {
        return new Instance(accessor);
    }

    private static final class Instance extends HybridAlgorithmWrapper.Instance {
        private final JFBBase.StateAccessor accessor;
        private final int[] indices;

        private Instance(JFBBase.StateAccessor accessor) {
            this.accessor = accessor;
            this.indices = accessor.getIndices();
        }

        @Override
        public boolean helperAHookCondition(int size, int obj) {
            switch (obj) {
                case 1: return false;
                case 2: return size < THRESHOLD_3D;
                default: return size < THRESHOLD_ALL;
            }
        }

        @Override
        public int helperAHook(int from, int until, int obj) {
            for (int left = from; left < until; ++left) {
                until = accessor.updateByPoint(indices[left], left + 1, until, obj);
            }
            return until;
        }

        @Override
        public boolean helperBHookCondition(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
            return helperAHookCondition(goodUntil - goodFrom + weakUntil - weakFrom, obj);
        }

        @Override
        public int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom) {
            for (int good = goodFrom, weakMin = weakFrom; good < goodUntil; ++good) {
                int goodIndex = indices[good];
                while (weakMin < weakUntil && indices[weakMin] < goodIndex) {
                    ++weakMin;
                }
                weakUntil = accessor.updateByPoint(goodIndex, weakMin, weakUntil, obj);
            }
            return weakUntil;
        }
    }
}
