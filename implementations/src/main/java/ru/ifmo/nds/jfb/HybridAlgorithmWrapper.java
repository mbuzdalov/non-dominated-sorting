package ru.ifmo.nds.jfb;

public abstract class HybridAlgorithmWrapper {
    public abstract boolean supportsMultipleThreads();
    public abstract String getName();
    public abstract Instance create(JFBBase.StateAccessor accessor);

    public static abstract class Instance {
        public abstract boolean helperAHookCondition(int size, int obj);
        public abstract boolean helperBHookCondition(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj);
        public abstract int helperAHook(int from, int until, int obj);
        public abstract int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom);
    }
}
