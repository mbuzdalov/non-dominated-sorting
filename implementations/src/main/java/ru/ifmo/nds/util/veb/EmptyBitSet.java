package ru.ifmo.nds.util.veb;

final class EmptyBitSet extends VanEmdeBoasSet {
    static VanEmdeBoasSet INSTANCE = new EmptyBitSet();

    private EmptyBitSet() {}

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public int min() {
        return Integer.MAX_VALUE;
    }

    @Override
    public int max() {
        return -1;
    }

    @Override
    public int prev(int index) {
        return -1;
    }

    @Override
    public int next(int index) {
        return Integer.MAX_VALUE;
    }

    @Override
    public void add(int index) {
        throw new UnsupportedOperationException("EmptyBitSet is immutable");
    }

    @Override
    public void remove(int index) {
        // do nothing
    }

    @Override
    public void clear() {
        // do nothing
    }

    @Override
    public int prevInclusively(int index) {
        return -1;
    }

    @Override
    public void setEnsuringMonotonicity(int index, int offset, int value, int[] values) {
        throw new UnsupportedOperationException("EmptyBitSet is immutable");
    }

    @Override
    void cleanupUpwards(int offset, int value, int[] values) {
        // do nothing
    }
}
