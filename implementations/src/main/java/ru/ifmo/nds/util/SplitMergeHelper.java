package ru.ifmo.nds.util;

public final class SplitMergeHelper {
    private final int[] scratchM, scratchR;

    public SplitMergeHelper(int size) {
        scratchM = new int[size];
        scratchR = new int[size];
    }

    public final int splitInTwo(double[] points, int[] indices,
                          int tempFrom, int from, int until, double median,
                          boolean equalToLeft, double minVal, double maxVal) {
        if (minVal == median && maxVal == median) {
            return equalToLeft ? until : from;
        } else if (minVal > median || !equalToLeft && minVal == median) {
            return from;
        } else if (maxVal < median || equalToLeft && maxVal == median) {
            return until;
        } else {
            int left = from, right = tempFrom;
            for (int i = from; i < until; ++i) {
                int ii = indices[i];
                double v = points[ii];
                if (v < median || (equalToLeft && v == median)) {
                    indices[left++] = ii;
                } else {
                    scratchR[right++] = ii;
                }
            }
            System.arraycopy(scratchR, tempFrom, indices, left, right - tempFrom);
            return left;
        }
    }

    public final long splitInThree(double[] points, int[] indices,
                                   int tempFrom, int from, int until, double median,
                                   double minVal, double maxVal) {
        if (minVal == median && maxVal == median) {
            return pack(from, until);
        } else if (minVal > median) {
            return pack(from, from);
        } else if (maxVal < median) {
            return pack(until, until);
        } else {
            int l = from, m = tempFrom, r = tempFrom;
            for (int i = from; i < until; ++i) {
                int ii = indices[i];
                double v = points[ii];
                if (v < median) {
                    indices[l++] = ii;
                } else if (v == median) {
                    scratchM[m++] = ii;
                } else {
                    scratchR[r++] = ii;
                }
            }
            System.arraycopy(scratchM, tempFrom, indices, l, m - tempFrom);
            System.arraycopy(scratchR, tempFrom, indices, l + m - tempFrom, r - tempFrom);
            return pack(l, m - tempFrom + l);
        }
    }

    public final int mergeTwo(int[] indices, int tempFrom, int fromLeft, int untilLeft, int fromRight, int untilRight) {
        int target = tempFrom;
        int l = fromLeft, r = fromRight;
        while (l < untilLeft && r < untilRight) {
            if (indices[l] <= indices[r]) {
                scratchM[target++] = indices[l++];
            } else {
                scratchM[target++] = indices[r++];
            }
        }
        int newR = fromLeft + (target - tempFrom) + untilLeft - l;
        if (r != newR && untilRight > r) {
            // copy the remainder of right to its place
            System.arraycopy(indices, r, indices, newR, untilRight - r);
        }
        if (l != fromLeft + (target - tempFrom) && untilLeft > l) {
            // copy the remainder of left to its place
            System.arraycopy(indices, l, indices, fromLeft + (target - tempFrom), untilLeft - l);
        }
        if (target > tempFrom) {
            // copy the merged part
            System.arraycopy(scratchM, tempFrom, indices, fromLeft, target - tempFrom);
        }
        return fromLeft + (target - tempFrom) + untilLeft - l + untilRight - r;
    }

    private static long pack(int mid, int right) {
        return (((long) (mid)) << 32) ^ right;
    }

    public static int extractMid(long packed) {
        return (int) (packed >>> 32);
    }

    public static int extractRight(long packed) {
        return (int) (packed);
    }
}
