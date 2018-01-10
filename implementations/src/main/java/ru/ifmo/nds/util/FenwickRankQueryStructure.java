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
    public RangeHandle createHandle(int storageStart, int from, int until, int[] indices, double[] keys) {
        return new RangeHandleImpl(storageStart, from, until, indices, keys);
    }

    private class RangeHandleImpl extends RankQueryStructure.RangeHandle {
        private final int size;
        private final int offset;

        private RangeHandleImpl(int storageStart, int from, int until, int[] indices, double[] k) {
            this.offset = storageStart;
            for (int i = from, j = offset; i < until; ++i, ++j) {
                keys[j] = k[indices[i]];
            }
            int storageEnd = storageStart + until - from;
            Arrays.sort(keys, storageStart, storageEnd);
            int realSize = 1;
            for (int i = storageStart + 1; i < storageEnd; ++i) {
                if (keys[i] != keys[i - 1]) {
                    keys[offset + realSize++] = keys[i];
                }
            }
            size = realSize;
            Arrays.fill(values, offset, offset + size, -1);
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
            int fwi = indexFor(key);
            while (fwi < size) {
                values[offset + fwi] = Math.max(values[offset + fwi], value);
                fwi |= fwi + 1;
            }
        }

        @Override
        public int getMaximumWithKeyAtMost(double key, int minimumMeaningfulAnswer) {
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
    }
}
