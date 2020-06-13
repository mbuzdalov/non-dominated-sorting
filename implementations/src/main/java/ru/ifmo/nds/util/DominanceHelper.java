package ru.ifmo.nds.util;

public final class DominanceHelper {
    private DominanceHelper() {}

    public static boolean strictlyDominates(double[] a, double[] b, int dim) {
        boolean hasSmaller = false;
        for (int i = 0; i < dim; ++i) {
            double ai = a[i], bi = b[i];
            if (ai > bi) {
                return false;
            }
            if (ai < bi) {
                hasSmaller = true;
            }
        }
        return hasSmaller;
    }

    public static boolean strictlyDominatesAssumingLexicographicallySmaller(double[] goodPoint, double[] weakPoint, int maxObj) {
        // Comparison in 0 makes no sense, due to goodPoint being lexicographically smaller than weakPoint.
        for (int i = maxObj; i > 0; --i) {
            if (goodPoint[i] > weakPoint[i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean strictlyDominatesAssumingNotEqual(double[] goodPoint, double[] weakPoint, int maxObj) {
        for (int i = maxObj; i >= 0; --i) {
            if (goodPoint[i] > weakPoint[i]) {
                return false;
            }
        }
        return true;
    }

    private static int dominanceComparisonHasSmaller(double[] a, double[] b, int from, int dim) {
        for (int i = from; i < dim; ++i) {
            if (a[i] > b[i]) {
                return 0;
            }
        }
        return -1;
    }

    private static int dominanceComparisonHasLarger(double[] a, double[] b, int from, int dim) {
        for (int i = from; i < dim; ++i) {
            if (a[i] < b[i]) {
                return 0;
            }
        }
        return 1;
    }

    public static int dominanceComparison(double[] a, double[] b, int dim) {
        for (int i = 0; i < dim; ++i) {
            double ai = a[i], bi = b[i];
            if (ai < bi) {
                return dominanceComparisonHasSmaller(a, b, i + 1, dim);
            }
            if (ai > bi) {
                return dominanceComparisonHasLarger(a, b, i + 1, dim);
            }
        }
        return 0;
    }
}
