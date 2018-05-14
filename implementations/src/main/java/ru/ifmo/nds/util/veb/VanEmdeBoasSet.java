package ru.ifmo.nds.util.veb;

/**
 * This is the set of integers implemented as a Van Emde Boas Tree.
 */
public abstract class VanEmdeBoasSet {
    public abstract boolean isEmpty();
    public abstract int min();
    public abstract int max();
    public abstract int prev(int index);
    public abstract int next(int index);

    public abstract boolean contains(int index);
    public abstract void add(int index);
    public abstract void remove(int index);

    public abstract void clear();

    public abstract int prevInclusively(int index);

    /**
     * Creates a new Van Emde Boas set that can fit integers from 0 until 2^scale.
     * @param scale the maximum number of bits in the numbers to store.
     * @return the newly created empty set.
     */
    public static VanEmdeBoasSet create(int scale) {
        switch (scale) {
            case 0:
            case 1:
            case 2:
            case 3:
            case 4:
            case 5:
                return new IntBitSet();
            case 6:
                return new LongBitSet();
            case 7:
            case 8:
            case 9:
            case 10:
                return new IntIntBitSet(scale);
            case 11:
                return new IntLongBitSet();
            case 12:
                return new LongLongBitSet();
            case 13:
                return new LongAnyBitSet();
            default:
                return new AnyAnyBitSet(scale);
        }
    }

    static int min(int value) {
        return Integer.numberOfTrailingZeros(value);
    }
    static int max(int value) {
        return 31 - Integer.numberOfLeadingZeros(value);
    }
    static int prev(int value, int index) {
        int mask = value & ~(-1 << index);
        return 31 - Integer.numberOfLeadingZeros(mask);
    }
    static int prevInclusively(int value, int index) {
        int mask = value & ~(-2 << index);
        return 31 - Integer.numberOfLeadingZeros(mask);
    }
    static int next(int value, int index) {
        int mask = value & ((-1 << index) << 1);
        return Integer.numberOfTrailingZeros(mask);
    }
    static boolean contains(int value, int index) {
        return ((value >>> index) & 1) == 1;
    }

    static int min(long value) {
        return Long.numberOfTrailingZeros(value);
    }
    static int max(long value) {
        return 63 - Long.numberOfLeadingZeros(value);
    }
    static int prev(long value, int index) {
        long mask = value & ~(-1L << index);
        return 63 - Long.numberOfLeadingZeros(mask);
    }
    static int prevInclusively(long value, int index) {
        long mask = value & ~(-2L << index);
        return 63 - Long.numberOfLeadingZeros(mask);
    }
    static int next(long value, int index) {
        long mask = value & ((-1L << index) << 1);
        return Long.numberOfTrailingZeros(mask);
    }
    static boolean contains(long value, int index) {
        return ((value >>> index) & 1) == 1;
    }
}
