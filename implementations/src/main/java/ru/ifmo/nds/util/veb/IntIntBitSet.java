package ru.ifmo.nds.util.veb;

final class IntIntBitSet extends VanEmdeBoasSet {
    private final int loBits;
    private final int loMask;
    private final int[] clusters;
    private final int limit;
    private int summary;

    private int min, max;

    IntIntBitSet(int scale) {
        limit = 1 << scale;
        min = limit;
        max = -1;
        loBits = scale / 2;
        loMask = (1 << loBits) - 1;
        clusters = new int[1 << (scale - loBits)];
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
        int h = hi(index), l = lo(index);
        int ch = clusters[h];
        if (((ch << ~l) << 1) == 0) {
            h = VanEmdeBoasSet.prev(summary, h);
            return h < 0 ? min : join(h, VanEmdeBoasSet.max(clusters[h]));
        } else {
            return join(h, VanEmdeBoasSet.prev(ch, l));
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
        int h = hi(index), l = lo(index);
        int ch = clusters[h];
        if (((ch >>> l) >>> 1) == 0) {
            h = VanEmdeBoasSet.next(summary, h);
            return h >= clusters.length ? max : join(h, VanEmdeBoasSet.min(clusters[h]));
        } else {
            return join(h, VanEmdeBoasSet.next(ch, l));
        }
    }

    @Override
    public boolean contains(int index) {
        if (max < 0) {
            return false;
        } else if (index == min || index == max) {
            return true;
        }
        return VanEmdeBoasSet.contains(clusters[hi(index)], lo(index));
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
            int l = lo(index), h = hi(index);
            if (clusters[h] == 0) {
                summary |= 1 << h;
            }
            clusters[h] |= 1 << l;
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
            int l = lo(index), h = hi(index);
            clusters[h] &= ~(1 << l);
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

    private int hi(int index) {
        return index >>> loBits;
    }
    private int lo(int index) {
        return index & loMask;
    }
    private int join(int hi, int lo) {
        return (hi << loBits) ^ lo;
    }
}
