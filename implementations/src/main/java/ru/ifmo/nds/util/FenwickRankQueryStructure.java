package ru.ifmo.nds.util;

import java.util.Arrays;

public final class FenwickRankQueryStructure extends RankQueryStructure {
    private final int[] keys;
    private final int[] values;

    public FenwickRankQueryStructure(int maximumSize) {
        keys = new int[maximumSize];
        values = new int[maximumSize];
    }

    @Override
    public RangeHandle createHandle(int storageStart, int from, int until, int[] indices, int[] keys) {
        return new RangeHandleImpl(storageStart, from, until, indices, keys);
    }

    private class RangeHandleImpl extends RankQueryStructure.RangeHandle {
        private final int size;
        private final int offset;

        private RangeHandleImpl(int storageStart, int from, int until, int[] indices, int[] k) {
            this.offset = storageStart;
            for (int i = from, j = offset; i < until; ++i, ++j) {
                keys[j] = k[indices[i]];
            }
            int storageEnd = storageStart + until - from;
            Arrays.sort(keys, storageStart, storageEnd);
            int uniqueEnd = offset + 1;
            int prev = keys[offset];
            for (int i = storageStart + 1; i < storageEnd; ++i) {
                int curr = keys[i];
                if (curr != prev) {
                    keys[uniqueEnd] = prev = curr;
                    ++uniqueEnd;
                }
            }
            Arrays.fill(values, offset, uniqueEnd, -1);
            size = uniqueEnd - offset;
        }

        private int indexFor(int key) {
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
        public void put(int key, int value) {
            int fwi = indexFor(key);
            while (fwi < size) {
                int idx = offset + fwi;
                values[idx] = Math.max(values[idx], value);
                fwi |= fwi + 1;
            }
        }

        @Override
        public int getMaximumWithKeyAtMost(int key, int minimumMeaningfulAnswer) {
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
