package ru.ifmo.nds.util;

import java.util.concurrent.ThreadLocalRandom;

public final class ArrayHelper {
    private ArrayHelper() {}

    public static void swap(int[] array, int a, int b) {
        int tmp = array[a];
        array[a] = array[b];
        array[b] = tmp;
    }

    public static void swap(double[] array, int a, int b) {
        double tmp = array[a];
        array[a] = array[b];
        array[b] = tmp;
    }

    public static boolean equal(double[] a, double[] b) {
        int al = a.length;
        if (al != b.length) {
            return false;
        }
        for (int i = 0; i < al; ++i) {
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

    public static double destructiveMedian(double[] array, int from, int until) {
        ThreadLocalRandom random = ThreadLocalRandom.current();
        int count = 0;
        int index = (from + until) >>> 1;
        while (from + 1 < until) {
            double pivot = array[++count > 20 ? random.nextInt(from, until) : (from + until) >>> 1];
            int l = from, r = until - 1;
            while (l <= r) {
                while (array[l] < pivot) ++l;
                while (array[r] > pivot) --r;
                if (l <= r) {
                    swap(array, l++, r--);
                }
            }
            if (index <= r) {
                until = r + 1;
            } else if (l <= index) {
                from = l;
            } else {
                break;
            }
        }
        return array[index];
    }

    public static int transplant(double[] source, int[] indices, int fromIndex, int untilIndex, double[] target, int targetFrom) {
        for (int i = fromIndex; i < untilIndex; ++i) {
            target[targetFrom++] = source[indices[i]];
        }
        return targetFrom;
    }

    public static int countSmaller(double[] array, int from, int until, double value) {
        int rv = 0;
        for (int i = from; i < until; ++i) {
            if (array[i] < value) {
                ++rv;
            }
        }
        return rv;
    }

    public static int countGreater(double[] array, int from, int until, double value) {
        int rv = 0;
        for (int i = from; i < until; ++i) {
            if (array[i] > value) {
                ++rv;
            }
        }
        return rv;
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
