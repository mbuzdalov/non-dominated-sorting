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

    public static boolean strictlyDominatesAssumingNotEqual(double[] goodPoint, double[] weakPoint, int dim) {
        for (int i = 0; i < dim; ++i) {
            if (goodPoint[i] > weakPoint[i]) {
                return false;
            }
        }
        return true;
    }

    private static int dominanceComparisonHelper(double[] a, double[] b, int dim, int maxDim, int returnOnEnd) {
        while (++dim < maxDim) {
            if (a[dim] < b[dim]) {
                return 0;
            }
        }
        return returnOnEnd;
    }

    public static int dominanceComparison(double[] a, double[] b, int dim) {
        for (int i = 0; i < dim; ++i) {
            double ai = a[i], bi = b[i];
            if (ai < bi) {
                return dominanceComparisonHelper(b, a, i, dim, -1);
            }
            if (ai > bi) {
                return dominanceComparisonHelper(a, b, i, dim, 1);
            }
        }
        return 0;
    }

    private static int dominanceComparisonFlatLess(double[] flatPoints, int p1Start, int p1End, int p2Start) {
        while (++p1Start < p1End) {
            if (flatPoints[p1Start] > flatPoints[++p2Start]) {
                return 0;
            }
        }
        return -1;
    }

    private static int dominanceComparisonFlatGreater(double[] flatPoints, int p1Start, int p1End, int p2Start) {
        while (++p1Start < p1End) {
            if (flatPoints[p1Start] < flatPoints[++p2Start]) {
                return 0;
            }
        }
        return +1;
    }

    public static int dominanceComparisonFlat(double[] flatPoints, int p1Start, int p1End, int p2Start) {
        while (p1Start < p1End) {
            double v1 = flatPoints[p1Start];
            double v2 = flatPoints[p2Start];
            if (v1 < v2) {
                return dominanceComparisonFlatLess(flatPoints, p1Start, p1End, p2Start);
            }
            if (v1 > v2) {
                return dominanceComparisonFlatGreater(flatPoints, p1Start, p1End, p2Start);
            }
            ++p1Start;
            ++p2Start;
        }
        return 0;
    }
}
