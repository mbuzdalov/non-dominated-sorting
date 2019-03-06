package ru.ifmo.nds.util.veb;

import java.util.Arrays;

final class AnyAnyBitSet extends VanEmdeBoasSet {
    private final int loBits;
    private final int loMask;
    private final VanEmdeBoasSet[] clusters;
    private final VanEmdeBoasSet summary;
    private final int limit;
    private final int clusterLimit;

    private int min, max;

    AnyAnyBitSet(int scale) {
        limit = 1 << scale;
        min = limit;
        max = -1;
        loBits = scale / 2;
        clusterLimit = 1 << loBits;
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
        if (index <= min) {
            return -1;
        }
        if (index > max) {
            return max;
        }
        int h = hi(index), l = lo(index);
        int q = clusters[h].prev(l);
        if (q == -1) {
            h = summary.prev(h);
            return h < 0 ? min : join(h, clusters[h].max());
        } else {
            return join(h, q);
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
        int q = clusters[h].prevInclusively(l);
        if (q == -1) {
            h = summary.prev(h);
            return h < 0 ? min : join(h, clusters[h].max());
        } else {
            return join(h, q);
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
        int q = clusters[h].next(l);
        if (q >= clusterLimit) {
            h = summary.next(h);
            return h >= clusters.length ? max : join(h, clusters[h].min());
        } else {
            return join(h, q);
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

    private boolean cleanupMidMax(int from, int offset, int value, int[] values) {
        if (!summary.isEmpty()) {
            for (int i = from == -1 ? summary.min() : summary.next(from); i < clusters.length; i = summary.next(i)) {
                VanEmdeBoasSet ci = clusters[i];
                ci.cleanupUpwards(offset + (i << loBits), value, values);
                if (ci.isEmpty()) {
                    summary.remove(i);
                } else {
                    return false;
                }
            }
        }
        return values[offset + max] <= value;
    }

    @Override
    public void setEnsuringMonotonicity(int index, int offset, int value, int[] values) {
        if (min == max) {
            // Only one element is stored. Consider where we fall...
            if (index < min) {
                values[offset + index] = value;
                min = index;
                if (values[offset + max] <= value) {
                    max = index;
                }
            } else if (index == min) {
                int oi = offset + index;
                if (values[oi] < value) {
                    values[oi] = value;
                }
            } else {
                if (values[offset + min] < value) {
                    values[offset + index] = value;
                    max = index;
                }
            }
        } else if (max == -1) {
            // The set was empty. Just set the value.
            min = index;
            max = index;
            values[offset + index] = value;
        } else  {
            if (index < min) {
                // First of all, insert ourselves.
                values[offset + index] = value;
                int oldMin = min;
                min = index;

                if (values[offset + oldMin] > value) {
                    // Add the old minimum and break out.
                    int h = hi(oldMin), l = lo(oldMin);
                    VanEmdeBoasSet ch = clusters[h];
                    if (ch == EmptyBitSet.INSTANCE) {
                        clusters[h] = ch = VanEmdeBoasSet.create(loBits);
                    }
                    ch.add(l);
                    summary.add(h);
                } else {
                    // Do not add the old minimum, as it is dominated.
                    if (cleanupMidMax(-1, offset, value, values)) {
                        max = min;
                    }
                }
            } else if (index == min) {
                int idx = offset + index;
                if (values[idx] < value) {
                    // Replace the value at the minimum, clean up the tail.
                    values[idx] = value;
                    if (cleanupMidMax(-1, offset, value, values)) {
                        max = min;
                    }
                }
            } else if (max <= index) {
                // We either replace max or add a new index after max
                if (values[offset + max] < value) {
                    values[offset + index] = value;
                    if (max != index) {
                        int oldMax = max;
                        max = index;
                        add(oldMax);
                    }
                }
            } else if (summary.isEmpty()) {
                if (values[offset + min] < value) {
                    values[offset + index] = value;
                    if (values[offset + max] > value) {
                        // Normal insertion
                        int h = hi(index), l = lo(index);
                        summary.add(h);
                        VanEmdeBoasSet ch = clusters[h];
                        if (ch == EmptyBitSet.INSTANCE) {
                            clusters[h] = ch = VanEmdeBoasSet.create(loBits);
                        }
                        ch.add(l);
                    } else {
                        // Replacement of max
                        max = index;
                    }
                }
            } else {
                int h = hi(index), l = lo(index);
                VanEmdeBoasSet ch = clusters[h];
                if (ch.min() > l) {
                    int hPrev = summary.prev(h);
                    int iPrev = hPrev == -1 ? min : join(hPrev, clusters[hPrev].max());
                    if (values[offset + iPrev] >= value) {
                        return;
                    }
                } else {
                    int lPrev = ch.prevInclusively(l);
                    if (values[offset + (h << loBits) + lPrev] >= value) {
                        return;
                    }
                }
                if (ch.isEmpty()) {
                    if (ch == EmptyBitSet.INSTANCE) {
                        clusters[h] = ch = VanEmdeBoasSet.create(loBits);
                    }
                    summary.add(h);
                }
                ch.setEnsuringMonotonicity(index & loMask, offset + (h << loBits), value, values);
                values[offset + index] = value;
                if (ch.max() == l) {
                    if (cleanupMidMax(h, offset, value, values)) {
                        max = index;
                        ch.remove(l);
                        if (ch.isEmpty()) {
                            summary.remove(h);
                        }
                    }
                }
            }
        }
    }

    @Override
    void cleanupUpwards(int offset, int value, int[] values) {
        if (values[offset + max] <= value) {
            clear();
        } else if (values[offset + min] <= value) {
            if (!summary.isEmpty()) {
                // need to cleanup at least something
                for (int i = summary.min(); i < clusters.length; i = summary.next(i)) {
                    VanEmdeBoasSet ci = clusters[i];
                    ci.cleanupUpwards(offset + (i << loBits), value, values);
                    if (!ci.isEmpty()) {
                        int min = ci.min();
                        this.min = min + (i << loBits);
                        ci.remove(min);
                        if (ci.isEmpty()) {
                            summary.remove(i);
                        }
                        return;
                    }
                    summary.remove(i);
                }
            }
            // summary is empty here
            min = max;
        }
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
