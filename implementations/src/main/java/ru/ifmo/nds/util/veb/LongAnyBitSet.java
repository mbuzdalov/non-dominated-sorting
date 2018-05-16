package ru.ifmo.nds.util.veb;

final class LongAnyBitSet extends VanEmdeBoasSet {
    private static final int limit = 1 << 13;
    private final long[] clusters;
    private IntIntBitSet summary;

    private int min, max;

    LongAnyBitSet() {
        min = limit;
        max = -1;
        clusters = new long[128];
        summary = new IntIntBitSet(7);
    }

    @Override
    public boolean isEmpty() {
        return max == -1;
    }

    @Override
    public int min() {
        return min;
    }

    @Override
    public int max() {
        return max;
    }

    @Override
    public int prev(int index) {
        if (index > max) {
            return max;
        }
        if (index <= min) {
            return -1;
        }
        int h = hi(index);
        long chs = (clusters[h] << ~index) << 1;
        if (chs == 0) {
            h = summary.prev(h);
            return h < 0 ? min : join(h, VanEmdeBoasSet.max(clusters[h]));
        } else {
            return index - 1 - Long.numberOfLeadingZeros(chs);
        }
    }

    @Override
    public int prevInclusively(int index) {
        if (index <= min) {
            // same as "index == min ? min : -1"
            return min | ((index - min) >> 31);
        }
        if (index >= max) {
            return max;
        }
        int h = hi(index);
        long chs = clusters[h] << ~index;
        if (chs == 0) {
            h = summary.prev(h);
            return h < 0 ? min : join(h, VanEmdeBoasSet.max(clusters[h]));
        } else {
            return index - Long.numberOfLeadingZeros(chs);
        }
    }

    @Override
    public int next(int index) {
        if (index < min) {
            return min;
        }
        if (index >= max) {
            return limit;
        }
        int h = hi(index);
        long chs = (clusters[h] >>> index) >>> 1;
        if (chs == 0) {
            h = summary.next(h);
            return h >= 128 ? max : join(h, VanEmdeBoasSet.min(clusters[h]));
        } else {
            return index + 1 + Long.numberOfTrailingZeros(chs);
        }
    }

    @Override
    public void add(int index) {
        if (max < 0) {
            min = max = index;
        } else if (min == max && index != min) {
            if (index < min) {
                min = index;
            } else {
                max = index;
            }
        } else if (index != min && index != max) {
            if (index < min) {
                int tmp = min;
                min = index;
                index = tmp;
            }
            if (index > max) {
                int tmp = max;
                max = index;
                index = tmp;
            }
            int h = hi(index);
            if (clusters[h] == 0) {
                summary.add(h);
            }
            clusters[h] |= 1L << index;
        }
    }

    @Override
    public void remove(int index) {
        if (index == min) {
            if (index == max) {
                min = limit;
                max = -1;
            } else {
                int newMin = next(min);
                if (newMin != max) {
                    remove(newMin);
                }
                min = newMin;
            }
        } else if (index == max) {
            int newMax = prev(max);
            if (newMax != min) {
                remove(newMax);
            }
            max = newMax;
        } else if (min < index && index < max) {
            int h = hi(index);
            clusters[h] &= ~(1L << index);
            if (clusters[h] == 0) {
                summary.remove(h);
            }
        }
    }

    @Override
    public void clear() {
        min = limit;
        max = -1;
        for (int i = summary.min(); i < 128; i = summary.next(i)) {
            clusters[i] = 0;
        }
        summary.clear();
    }

    private static int hi(int index) {
        return index >>> 6;
    }
    private static int join(int hi, int lo) {
        return (hi << 6) ^ lo;
    }
}
