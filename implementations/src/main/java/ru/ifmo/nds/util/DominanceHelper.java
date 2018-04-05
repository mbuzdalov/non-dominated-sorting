package ru.ifmo.nds.util;

public final class DominanceHelper {
    private DominanceHelper() {}

    public static boolean strictlyDominates(double[] a, double[] b, int dim) {
        return strictlyDominates1(a, b, dim);
    }

    public static int dominanceComparison(double[] a, double[] b, int dim) {
        return dominanceComparison1(a, b, dim);
    }

    public static boolean strictlyDominates1(double[] a, double[] b, int dim) {
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

    public static boolean strictlyDominates2(double[] a, double[] b, int dim) {
        boolean hasSmaller = false;
        for (int i = 0; i < dim; ++i) {
            double ai = a[i], bi = b[i];
            if (ai > bi) {
                return false;
            }
            hasSmaller |= ai < bi;
        }
        return hasSmaller;
    }

    public static int dominanceComparison1(double[] a, double[] b, int dim) {
        boolean hasSmaller = false;
        boolean hasGreater = false;
        for (int i = 0; i < dim; ++i) {
            double ai = a[i], bi = b[i];
            if (ai < bi) {
                if (hasGreater) {
                    return 0;
                }
                hasSmaller = true;
            } else if (ai > bi) {
                if (hasSmaller) {
                    return 0;
                }
                hasGreater = true;
            }
        }
        return hasSmaller ? -1 : hasGreater ? 1 : 0;
    }

    public static int dominanceComparison2(double[] a, double[] b, int dim) {
        boolean hasSmaller = false;
        boolean hasGreater = false;
        for (int i = 0; i < dim; ++i) {
            double ai = a[i], bi = b[i];
            if (ai < bi) {
                hasSmaller = true;
            } else if (ai > bi) {
                hasGreater = true;
            }
            if (hasSmaller && hasGreater) {
                return 0;
            }
        }
        return hasSmaller ? -1 : hasGreater ? 1 : 0;
    }
}
