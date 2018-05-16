package ru.ifmo.nds.util.veb;

import java.util.Arrays;

final class AnyAnyBitSet extends VanEmdeBoasSet {
    private final int loBits;
    private final int loMask;
    private final VanEmdeBoasSet[] clusters;
    private final VanEmdeBoasSet summary;
    private final int limit;

    private int min, max;

    AnyAnyBitSet(int scale) {
        limit = 1 << scale;
        min = limit;
        max = -1;
        loBits = scale / 2;
        loMask = (1 << loBits) - 1;
        clusters = new VanEmdeBoasSet[1 << (scale - loBits)];
        Arrays.fill(clusters, EmptyBitSet.INSTANCE);
        summary = VanEmdeBoasSet.create(scale - loBits);
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
        VanEmdeBoasSet ch = clusters[h];
        if (l <= ch.min()) {
            h = summary.prev(h);
            return h < 0 ? min : join(h, clusters[h].max());
        } else {
            return join(h, ch.prev(l));
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
        int h = hi(index), l = lo(index);
        VanEmdeBoasSet ch = clusters[h];
        if (l < ch.min()) {
            h = summary.prev(h);
            return h < 0 ? min : join(h, clusters[h].max());
        } else {
            return join(h, ch.prevInclusively(l));
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
        int h = hi(index), l = lo(index);
        VanEmdeBoasSet ch = clusters[h];
        if (l >= ch.max()) {
            h = summary.next(h);
            return h >= clusters.length ? max : join(h, clusters[h].min());
        } else {
            return join(h, ch.next(l));
        }
    }

    @Override
    public boolean contains(int index) {
        return index == min || index == max || index > min && index < max && clusters[hi(index)].contains(lo(index));
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
            VanEmdeBoasSet ch = clusters[h];
            if (ch == EmptyBitSet.INSTANCE) {
                clusters[h] = ch = VanEmdeBoasSet.create(loBits);
            }
            if (ch.isEmpty()) {
                summary.add(h);
            }
            ch.add(l);
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
            VanEmdeBoasSet ch = clusters[h];
            ch.remove(l);
            if (ch.isEmpty()) {
                summary.remove(h);
            }
        }
    }

    @Override
    public void clear() {
        min = limit;
        max = -1;
        for (int i = summary.min(); i < clusters.length; i = summary.next(i)) {
            clusters[i].clear();
        }
        summary.clear();
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
