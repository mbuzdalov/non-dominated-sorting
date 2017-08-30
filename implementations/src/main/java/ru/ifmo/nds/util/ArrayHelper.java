package ru.ifmo.nds.util;

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
}
