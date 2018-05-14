package ru.ifmo.nds.util.veb;

final class IntIntBitSet extends VanEmdeBoasSet {
    private static final int limit = 1 << 10;
    private final int[] clusters;
    private int summary;
    private int min, max;

    IntIntBitSet(int scale) {
        min = limit;
        max = -1;
        clusters = new int[1 << (scale - 5)];
        summary = 0;
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
        int chs = (clusters[h] << ~index) << 1;
        if (chs == 0) {
            h = VanEmdeBoasSet.prev(summary, h);
            return h < 0 ? min : join(h, VanEmdeBoasSet.max(clusters[h]));
        } else {
            return index - 1 - Integer.numberOfLeadingZeros(chs);
        }
    }

    @Override
    public int prevInclusively(int index) {
        if (index >= max) {
            return max;
        }
        if (index <= min) {
            return index == min ? min : -1;
        }
        int h = hi(index);
        int chs = clusters[h] << ~index;
        if (chs == 0) {
            h = VanEmdeBoasSet.prev(summary, h);
            return h < 0 ? min : join(h, VanEmdeBoasSet.max(clusters[h]));
        } else {
            return index - Integer.numberOfLeadingZeros(chs);
        }
    }

    @Override
    public int next(int index) {
        if (index >= max) {
            return limit;
        }
        if (index < min) {
            return min;
        }
        int h = hi(index);
        int chs = (clusters[h] >>> index) >>> 1;
        if (chs == 0) {
            h = VanEmdeBoasSet.next(summary, h);
            return h >= clusters.length ? max : join(h, VanEmdeBoasSet.min(clusters[h]));
        } else {
            return index + 1 + Integer.numberOfTrailingZeros(chs);
        }
    }

    @Override
    public boolean contains(int index) {
        if (max < 0) {
            return false;
        } else if (index == min || index == max) {
            return true;
        }
        return VanEmdeBoasSet.contains(clusters[hi(index)], index);
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
                summary |= 1 << h;
            }
            clusters[h] |= 1 << index;
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
            clusters[h] &= ~(1 << index);
            if (clusters[h] == 0) {
                summary &= ~(1 << h);
            }
        }
    }

    @Override
    public void clear() {
        min = limit;
        max = -1;
        for (int i = VanEmdeBoasSet.min(summary); i < clusters.length; i = VanEmdeBoasSet.next(summary, i)) {
            clusters[i] = 0;
        }
        summary = 0;
    }

    private static int hi(int index) {
        return index >>> 5;
    }
    private static int join(int hi, int lo) {
        return (hi << 5) ^ lo;
    }
}
