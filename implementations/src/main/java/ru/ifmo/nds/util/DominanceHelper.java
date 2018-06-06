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

    public static boolean strictlyDominatesAssumingNotSame(double[] goodPoint, double[] weakPoint, int maxObj) {
        // Comparison in 0 makes no sense, as due to goodIndex < weakIndex the points are <= in this coordinate.
        for (int i = maxObj; i > 0; --i) {
            if (goodPoint[i] > weakPoint[i]) {
                return false;
            }
        }
        return true;
    }


    public static int dominanceComparison(double[] a, double[] b, int dim) {
        boolean hasSmaller = false;
        boolean hasGreater = false;
        for (int i = 0; i < dim; ++i) {
            double ai = a[i], bi = b[i];
            if (ai < bi) {
                hasSmaller = true;
            }
            // no "else if" because the code becomes way slower on inputs where "a" dominates "b".
            if (ai > bi) {
                hasGreater = true;
            }
            if (hasSmaller && hasGreater) {
                return 0;
            }
        }
        return hasSmaller ? -1 : hasGreater ? 1 : 0;
    }
}
