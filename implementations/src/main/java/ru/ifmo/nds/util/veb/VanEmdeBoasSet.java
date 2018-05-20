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

    public abstract void add(int index);
    public abstract void remove(int index);

    public abstract void clear();

    public abstract int prevInclusively(int index);

    public abstract void setEnsuringMonotonicity(int index, int offset, int value, int[] values);
    abstract void cleanupUpwards(int offset, int value, int[] values);

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

    static int setEnsuringMonotonicity(int value, int index, int offset, int newValue, int[] allValues) {
        int allGreaterThanIndexMask = (-1 << index) << 1;
        int upToIndex = value & ~allGreaterThanIndexMask;
        if (upToIndex != 0 && allValues[offset + 31 - Integer.numberOfLeadingZeros(upToIndex)] >= newValue) {
            // There is someone at or before the index with the greater value.
            // Cannot insert the current value.
            return value;
        }
        // Set the requested index to the requested value.
        allValues[offset + index] = newValue;
        value |= 1 << index;

        // Try dropping higher indices if they are smaller. Changing the contents of allValues is not needed.
        while (true) {
            int greaterThan = value & allGreaterThanIndexMask;
            if (greaterThan == 0) {
                break;
            }
            int particularIndex = Integer.numberOfTrailingZeros(greaterThan);
            if (allValues[offset + particularIndex] > newValue) {
                break;
            }
            value ^= 1 << particularIndex;
        }
        return value;
    }

    static long setEnsuringMonotonicity(long value, int index, int offset, int newValue, int[] allValues) {
        long allGreaterThanIndexMask = (-1L << index) << 1;
        long upToIndex = value & ~allGreaterThanIndexMask;
        if (upToIndex != 0 && allValues[offset + 63 - Long.numberOfLeadingZeros(upToIndex)] >= newValue) {
            // There is someone at or before the index with the greater value.
            // Cannot insert the current value.
            return value;
        }
        // Set the requested index to the requested value.
        allValues[offset + index] = newValue;
        value |= 1L << index;

        // Try dropping higher indices if they are smaller. Changing the contents of allValues is not needed.
        while (true) {
            long greaterThan = value & allGreaterThanIndexMask;
            if (greaterThan == 0) {
                break;
            }
            int particularIndex = Long.numberOfTrailingZeros(greaterThan);
            if (allValues[offset + particularIndex] > newValue) {
                break;
            }
            value ^= 1L << particularIndex;
        }
        return value;
    }

    static int cleanupUpwards(int value, int offset, int newValue, int[] allValues) {
        int localMax = VanEmdeBoasSet.max(value);
        if (allValues[offset + localMax] <= newValue) {
            return 0;
        }
        if (value != 1 << localMax) {
            // The good point is somewhere inside
            for (int j = VanEmdeBoasSet.min(value);
                 j < localMax && allValues[offset + j] <= newValue;
                 j = VanEmdeBoasSet.next(value, j)) {
                value ^= 1 << j;
            }
        } // else this is just a single point, and we don't do anything.
        return value;
    }

    static long cleanupUpwards(long value, int offset, int newValue, int[] allValues) {
        int localMax = VanEmdeBoasSet.max(value);
        if (allValues[offset + localMax] <= newValue) {
            return 0;
        }
        if (value != 1L << localMax) {
            // The good point is somewhere inside
            for (int j = VanEmdeBoasSet.min(value);
                 j < localMax && allValues[offset + j] <= newValue;
                 j = VanEmdeBoasSet.next(value, j)) {
                value ^= 1L << j;
            }
        } // else this is just a single point, and we don't do anything.
        return value;
    }
}
