package ru.ifmo.nds.util.veb;

final class LongBitSet extends VanEmdeBoasSet {
    private long value;

    @Override
    public boolean isEmpty() {
        return value == 0;
    }

    @Override
    public int min() {
        return VanEmdeBoasSet.min(value);
    }

    @Override
    public int max() {
        return VanEmdeBoasSet.max(value);
    }

    @Override
    public int prev(int index) {
        return VanEmdeBoasSet.prev(value, index);
    }

    @Override
    public int next(int index) {
        return VanEmdeBoasSet.next(value, index);
    }

    @Override
    public void add(int index) {
        value |= 1L << index;
    }

    @Override
    public void remove(int index) {
        value &= ~(1L << index);
    }

    @Override
    public void clear() {
        value = 0;
    }

    @Override
    public int prevInclusively(int index) {
        return VanEmdeBoasSet.prevInclusively(value, index);
    }

    @Override
    public void setEnsuringMonotonicity(int index, int offset, int newValue, int[] allValues) {
        value = VanEmdeBoasSet.setEnsuringMonotonicity(value, index, offset, newValue, allValues);
    }

    @Override
    void cleanupUpwards(int offset, int value, int[] values) {
        this.value = VanEmdeBoasSet.cleanupUpwards(this.value, offset, value, values);
    }
}
