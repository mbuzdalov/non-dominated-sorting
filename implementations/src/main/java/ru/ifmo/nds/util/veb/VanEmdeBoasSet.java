package ru.ifmo.nds.util.veb;

/**
 * This is the set of integers implemented as a Van Emde Boas Tree.
 */
public abstract class VanEmdeBoasSet {
    private final int scale;

    VanEmdeBoasSet(int scale) {
        this.scale = scale;
    }

    void checkRange(int index) {
        if (index < 0 || (index >>> scale) != 0) {
            throw new IndexOutOfBoundsException("Index " + index + ", scale " + scale);
        }
    }

    public abstract boolean isEmpty();
    public abstract int min();
    public abstract int max();
    public abstract int prev(int index);
    public abstract int next(int index);

    public abstract boolean contains(int index);
    public abstract void add(int index);
    public abstract void remove(int index);

    /**
     * Creates a new Van Emde Boas set that can fit integers from 0 until 2^scale.
     * @param scale the maximum number of bits in the numbers to store.
     * @return the newly created empty set.
     */
    public static VanEmdeBoasSet create(int scale) {
        if (scale <= 6) {
            return new SingleLongBitSet(scale);
        } else {
            return new LargeBitSet(scale);
        }
    }

    private static final class LargeBitSet extends VanEmdeBoasSet {
        private final int loBits;
        private final int loMask;
        private final VanEmdeBoasSet[] clusters;
        private final VanEmdeBoasSet summary;

        private int min = -1, max = -1;

        LargeBitSet(int scale) {
            super(scale);
            loBits = scale / 2;
            loMask = (1 << loBits) - 1;
            clusters = new VanEmdeBoasSet[1 << (scale - loBits)];
            summary = VanEmdeBoasSet.create(scale - loBits);
        }

        @Override
        public boolean isEmpty() {
            return min == -1;
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
            checkRange(index);
            if (index > max) {
                return max;
            }
            if (index <= min) {
                return -1;
            }
            int h = hi(index), l = lo(index);
            VanEmdeBoasSet ch = clusters[h];
            if (ch == null || ch.isEmpty() || l <= ch.min()) {
                h = summary.prev(h);
                return h == -1 ? min : join(h, clusters[h].max());
            } else {
                return join(h, ch.prev(l));
            }
        }

        @Override
        public int next(int index) {
            checkRange(index);
            if (index >= max) {
                return -1;
            }
            if (index < min) {
                return min;
            }
            int h = hi(index), l = lo(index);
            VanEmdeBoasSet ch = clusters[h];
            if (ch == null || l >= ch.max()) {
                h = summary.next(h);
                return h == -1 ? max : join(h, clusters[h].min());
            } else {
                return join(h, ch.next(l));
            }
        }

        @Override
        public boolean contains(int index) {
            checkRange(index);
            if (min == -1) {
                return false;
            } else if (index == min || index == max) {
                return true;
            }
            VanEmdeBoasSet ch = clusters[hi(index)];
            return ch != null && ch.contains(lo(index));
        }

        @Override
        public void add(int index) {
            checkRange(index);
            if (min == -1) {
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
                if (ch == null) {
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
            checkRange(index);
            if (index == min) {
                if (index == max) {
                    min = max = -1;
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
                if (ch != null) {
                    ch.remove(l);
                    if (ch.isEmpty()) {
                        summary.remove(h);
                    }
                }
            }
        }

        private int hi(int index) {
            return index >>> loBits;
        }
        private int lo(int index) {
            return index & loMask;
        }
        private int join(int hi, int lo) {
            if (hi < 0 || lo < 0) {
                throw new AssertionError();
            }
            return (hi << loBits) ^ lo;
        }
    }

    private static final class SingleLongBitSet extends VanEmdeBoasSet {
        private long value;

        private SingleLongBitSet(int scale) {
            super(scale);
        }

        @Override
        public boolean isEmpty() {
            return value == 0;
        }

        @Override
        public int min() {
            return value == 0 ? -1 : Long.numberOfTrailingZeros(value);
        }

        @Override
        public int max() {
            return value == 0 ? -1 : 63 - Long.numberOfLeadingZeros(value);
        }

        @Override
        public int prev(int index) {
            checkRange(index);
            long mask = value & ~(-1L << index);
            return mask == 0 ? -1 : 63 - Long.numberOfLeadingZeros(mask);
        }

        @Override
        public int next(int index) {
            checkRange(index);
            if (index == 63) {
                return -1;
            }
            ++index;
            long mask = value & (-1L << index);
            return mask == 0 ? -1 : Long.numberOfTrailingZeros(mask);
        }

        @Override
        public boolean contains(int index) {
            checkRange(index);
            return ((value >>> index) & 1) == 1;
        }

        @Override
        public void add(int index) {
            checkRange(index);
            value |= 1L << index;
        }

        @Override
        public void remove(int index) {
            checkRange(index);
            value &= ~(1L << index);
        }
    }
}
