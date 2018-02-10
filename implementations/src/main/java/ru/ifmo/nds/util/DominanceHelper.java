package ru.ifmo.nds.util;

public final class DominanceHelper {
    private DominanceHelper() {}

    private static final int HAS_LESS_MASK = 1;
    private static final int HAS_GREATER_MASK = 2;

    private static final int[] REINDEX = { 0, -1, 1, 0 };

    public static boolean strictlyDominates(double[] a, double[] b, int dim) {
        return detailedDominanceComparison(a, b, dim, HAS_GREATER_MASK) == HAS_LESS_MASK;
    }

    public static int dominanceComparison(double[] a, double[] b, int dim) {
        int rv = detailedDominanceComparison(a, b, dim, HAS_GREATER_MASK | HAS_LESS_MASK);
        return REINDEX[rv];
    }

    private static int detailedDominanceComparison(double[] a, double[] b, int dim, int breakMask) {
        int result = 0;
        for (int i = 0; i < dim; ++i) {
            double ai = a[i], bi = b[i];
            if (ai < bi) {
                result |= HAS_LESS_MASK;
            } else if (ai > bi) {
                result |= HAS_GREATER_MASK;
            }
            if ((result & breakMask) == breakMask) {
                break;
            }
        }
        return result;
    }
}
