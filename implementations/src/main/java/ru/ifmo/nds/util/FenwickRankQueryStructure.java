package ru.ifmo.nds.util;

import java.util.Arrays;

public final class FenwickRankQueryStructure extends RankQueryStructure {
    private final double[] keys;
    private final int[] values;

    public FenwickRankQueryStructure(int maximumSize) {
        keys = new double[maximumSize];
        values = new int[maximumSize];
    }

    @Override
    public RangeHandle createHandle(int from, int until) {
        return new RangeHandleImpl(from, until);
    }

    private class RangeHandleImpl extends RankQueryStructure.RangeHandle {
        private int size = 0;
        private boolean initialized = false;
        private final int offset;
        private final int limit;

        private RangeHandleImpl(int offset, int limit) {
            this.offset = offset;
            this.limit = limit;
        }

        @Override
        public boolean needsPossibleKeys() {
            return true;
        }

        @Override
        public void addPossibleKey(double key) {
            if (initialized) {
                throw new IllegalStateException("addPossibleKey(double) called in the initialized mode");
            }
            if (offset + size >= limit) {
                throw new AssertionError();
            }
            keys[offset + size++] = key;
        }

        @Override
        public void init() {
            if (initialized) {
                throw new IllegalStateException("init() called in the initialized mode");
            }
            Arrays.sort(keys, offset, offset + size);
            int realSize = 1;
            for (int i = 1; i < size; ++i) {
                if (keys[offset + i] != keys[offset + i - 1]) {
                    keys[offset + realSize++] = keys[offset + i];
                }
            }
            size = realSize;
            Arrays.fill(values, offset, offset + size, -1);

            initialized = true;
        }

        private int indexFor(double key) {
            int left = -1, right = size;
            while (right - left > 1) {
                int mid = (left + right) >>> 1;
                if (keys[offset + mid] <= key) {
                    left = mid;
                } else {
                    right = mid;
                }
            }
            return left;

        }

        @Override
        public void put(double key, int value) {
            if (!initialized) {
                throw new IllegalStateException("put(double, int) called in the preparation mode");
            }
            int fwi = indexFor(key);
            while (fwi < size) {
                values[offset + fwi] = Math.max(values[offset + fwi], value);
                fwi |= fwi + 1;
            }
        }

        @Override
        public int getMaximumWithKeyAtMost(double key, int minimumMeaningfulAnswer) {
            if (!initialized) {
                throw new IllegalStateException("getMaximumWithKeyAtMost(double) called in the preparation mode");
            }

            int fwi = indexFor(key);
            if (fwi >= size || fwi < 0) {
                return -1;
            } else {
                int rv = -1;
                while (fwi >= 0) {
                    rv = Math.max(rv, values[offset + fwi]);
                    fwi = (fwi & (fwi + 1)) - 1;
                }
                return rv;
            }
        }

        @Override
        public void clear() {
            if (!initialized) {
                throw new IllegalStateException("clear() called in the preparation mode");
            }

            initialized = false;
            size = 0;
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }
}
