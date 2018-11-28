package ru.ifmo.nds.util;

public final class ArrayHelper {
    private ArrayHelper() {}

    public static void swap(int[] array, int a, int b) {
        int tmp = array[a];
        array[a] = array[b];
        array[b] = tmp;
    }

    public static boolean equal(final double[] a, final double[] b, final int prefixLength) {
        for (int i = 0; i < prefixLength; ++i) {
            if (a[i] != b[i]) {
                return false;
            }
        }
        return true;
    }

    public static void fillIdentity(int[] array, int n) {
        for (int i = 0; i < n; ++i) {
            array[i] = i;
        }
    }

    public static void fillIdentity(int[] array, int n, int offset) {
        for (int i = 0, v = offset; i < n; ++i, ++v) {
            array[i] = v;
        }
    }

    public static double destructiveMedian(double[] array, int from, int until) {
        int index = (from + until) >>> 1;
        int to = until - 1;
        while (from < to) {
            double pivot = array[(from + to) >>> 1];
            if (from + 4 < to) {
                double mid = (array[from] + array[to]) / 2;
                pivot = (pivot + mid) / 2;
            }
            double vl, vr;
            int l = from, r = to;
            do {
                while ((vl = array[l]) < pivot) ++l;
                while ((vr = array[r]) > pivot) --r;
                if (l <= r) {
                    array[l] = vr;
                    array[r] = vl;
                    ++l;
                    --r;
                }
            } while (l <= r);
            if (index < r) {
                to = r;
            } else if (l < index) {
                from = l;
            } else if (r == index) {
                return max(array, from, r + 1);
            } else if (l == index) {
                return min(array, l, to + 1);
            } else {
                return array[index];
            }
        }

        return array[index];
    }

    public static int transplant(double[] source, int[] indices, int fromIndex, int untilIndex, double[] target, int targetFrom) {
        for (int i = fromIndex; i < untilIndex; ++i, ++targetFrom) {
            target[targetFrom] = source[indices[i]];
        }
        return targetFrom;
    }

    public static double max(double[] array, int from, int until) {
        if (from >= until) {
            return Double.NEGATIVE_INFINITY;
        } else {
            double rv = array[from];
            for (int i = from + 1; i < until; ++i) {
                double v = array[i];
                if (rv < v) {
                    rv = v;
                }
            }
            return rv;
        }
    }

    public static double min(double[] array, int from, int until) {
        if (from >= until) {
            return Double.POSITIVE_INFINITY;
        } else {
            double rv = array[from];
            for (int i = from + 1; i < until; ++i) {
                double v = array[i];
                if (rv > v) {
                    rv = v;
                }
            }
            return rv;
        }
    }
}
