package ru.ifmo.nds.util;

import java.util.Arrays;

public final class SplitMergeHelper {
    private final int[] scratchM, scratchR;

    public SplitMergeHelper(int size) {
        scratchM = new int[size];
        scratchR = new int[size];
    }

    public final int splitInTwo(double[] points, int[] indices,
                                int tempFrom, int from, int until, double median,
                                boolean equalToLeft) {
        int left = from, right = tempFrom;
        for (int i = from; i < until; ++i) {
            int ii = indices[i];
            double v = points[ii];
            if (v < median || (equalToLeft && v == median)) {
                indices[left] = ii;
                ++left;
            } else {
                scratchR[right] = ii;
                ++right;
            }
        }
        System.arraycopy(scratchR, tempFrom, indices, left, right - tempFrom);
        return left;
    }

    public final long splitInThree(double[] points, int[] indices,
                                   int tempFrom, int from, int until, double median) {
        int l = from, m = tempFrom, r = tempFrom;
        for (int i = from; i < until; ++i) {
            int ii = indices[i];
            double v = points[ii];
            if (v < median) {
                indices[l] = ii;
                ++l;
            } else if (v == median) {
                scratchM[m] = ii;
                ++m;
            } else {
                scratchR[r] = ii;
                ++r;
            }
        }
        System.arraycopy(scratchM, tempFrom, indices, l, m - tempFrom);
        System.arraycopy(scratchR, tempFrom, indices, l + m - tempFrom, r - tempFrom);
        return pack(l, m - tempFrom + l);
    }

    public final int mergeThree(int[] indices, int tempFrom,
                                int fromLeft, int untilLeft,
                                int fromMid, int untilMid,
                                int fromRight, int untilRight) {
        if (fromMid != untilMid) {
            untilLeft = mergeTwo(indices, tempFrom, fromLeft, untilLeft, fromMid, untilMid);
        }
        return mergeTwo(indices, tempFrom, fromLeft, untilLeft, fromRight, untilRight);
    }

    private int mergeTwo(int[] indices, int tempFrom, int fromLeft, int untilLeft, int fromRight, int untilRight) {
        if (fromRight == untilRight) {
            return untilLeft;
        }
        fromLeft = -Arrays.binarySearch(indices, fromLeft, untilLeft, indices[fromRight]) - 1;
        int target = tempFrom;
        int l = fromLeft, r = fromRight;
        if (l < untilLeft && r < untilRight) {
            int il = indices[l];
            int ir = indices[r];
            while (true) {
                if (il <= ir) {
                    scratchM[target] = il;
                    ++target;
                    if (++l == untilLeft) {
                        break;
                    }
                    il = indices[l];
                } else {
                    scratchM[target] = ir;
                    ++target;
                    if (++r == untilRight) {
                        break;
                    }
                    ir = indices[r];
                }
            }
        }
        int newR = fromLeft + (target - tempFrom) + untilLeft - l;
        if (r != newR && untilRight > r) {
            // copy the remainder of right to its place
            System.arraycopy(indices, r, indices, newR, untilRight - r);
        }
        if (newR != untilLeft && untilLeft > l) {
            // copy the remainder of left to its place
            System.arraycopy(indices, l, indices, fromLeft + (target - tempFrom), untilLeft - l);
        }
        if (target > tempFrom) {
            // copy the merged part
            System.arraycopy(scratchM, tempFrom, indices, fromLeft, target - tempFrom);
        }
        return newR + untilRight - r;
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
