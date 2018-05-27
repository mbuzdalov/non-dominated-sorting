package ru.ifmo.nds.util;

import java.util.Arrays;

public final class FenwickRankQueryStructureDouble extends RankQueryStructureDouble {
    private final double[] keys;
    private final int[] values;

    public FenwickRankQueryStructureDouble(int maximumSize) {
        keys = new double[maximumSize];
        values = new int[maximumSize];
    }

    @Override
    public String getName() {
        return "Fenwick Tree for non-compressed coordinates";
    }

    @Override
    public int maximumPoints() {
        return keys.length;
    }

    @Override
    public boolean supportsMultipleThreads() {
        return true;
    }

    @Override
    public RangeHandle createHandle(int storageStart, int from, int until, int[] indices, double[] keys) {
        return new RangeHandleImpl(storageStart, from, until, indices, keys);
    }

    private class RangeHandleImpl extends RangeHandle {
        private final int size;
        private final int offset;

        private RangeHandleImpl(int storageStart, int from, int until, int[] indices, double[] k) {
            this.offset = storageStart;
            for (int i = from, j = offset; i < until; ++i, ++j) {
                keys[j] = k[indices[i]];
            }
            int storageEnd = storageStart + until - from;
            Arrays.sort(keys, storageStart, storageEnd);
            int uniqueEnd = offset + 1;
            double prev = keys[offset];
            for (int i = storageStart + 1; i < storageEnd; ++i) {
                double curr = keys[i];
                if (curr != prev) {
                    keys[uniqueEnd] = prev = curr;
                    ++uniqueEnd;
                }
            }
            Arrays.fill(values, offset, uniqueEnd, -1);
            size = uniqueEnd - offset;
        }

        private int indexFor(double key) {
            int left = offset - 1, right = offset + size;
            while (right - left > 1) {
                int mid = (left + right) >>> 1;
                if (keys[mid] <= key) {
                    left = mid;
                } else {
                    right = mid;
                }
            }
            return left - offset;
        }

        @Override
        public RangeHandle put(double key, int value) {
            int fwi = indexFor(key);
            while (fwi < size) {
                int idx = offset + fwi;
                values[idx] = Math.max(values[idx], value);
                fwi |= fwi + 1;
            }
            return this;
        }

        @Override
        public int getMaximumWithKeyAtMost(double key, int minimumMeaningfulAnswer) {
            int fwi = indexFor(key);
            int rv = -1;
            while (fwi >= 0) {
                rv = Math.max(rv, values[offset + fwi]);
                fwi = (fwi & (fwi + 1)) - 1;
            }
            return rv;
        }
    }
}
