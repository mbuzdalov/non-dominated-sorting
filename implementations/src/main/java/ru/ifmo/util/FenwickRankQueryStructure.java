package ru.ifmo.util;

import java.util.Arrays;

public class FenwickRankQueryStructure extends RankQueryStructure {
    private final double[] keys;
    private final int[] values;
    private int size;
    private boolean initialized;

    public FenwickRankQueryStructure(int maximumSize) {
        keys = new double[maximumSize];
        values = new int[maximumSize];
        size = 0;
        initialized = false;
    }

    @Override
    public void addPossibleKey(double key) {
        if (initialized) {
            throw new IllegalStateException("addPossibleKey(double) called in the initialized mode");
        }
        keys[size++] = key;
    }

    @Override
    public void init() {
        if (initialized) {
            throw new IllegalStateException("init() called in the initialized mode");
        }
        Arrays.sort(keys, 0, size);
        int realSize = 1;
        for (int i = 1; i < size; ++i) {
            if (keys[i] != keys[i - 1]) {
                keys[realSize++] = keys[i];
            }
        }
        size = realSize;
        Arrays.fill(values, 0, size, -1);

        initialized = true;
    }

    private int indexFor(double key) {
        int left = -1, right = size;
        while (right - left > 1) {
            int mid = (left + right) >>> 1;
            if (keys[mid] <= key) {
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
            values[fwi] = Math.max(values[fwi], value);
            fwi |= fwi + 1;
        }
    }

    @Override
    public int getMaximumWithKeyAtMost(double key) {
        if (!initialized) {
            throw new IllegalStateException("getMaximumWithKeyAtMost(double) called in the preparation mode");
        }

        int fwi = indexFor(key);
        if (fwi >= size || fwi < 0) {
            return -1;
        } else {
            int rv = -1;
            while (fwi >= 0) {
                rv = Math.max(rv, values[fwi]);
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
