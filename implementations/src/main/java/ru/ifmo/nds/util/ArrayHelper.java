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

    public static final int TRANSPLANT_LEFT_NOT_GREATER = 0;
    public static final int TRANSPLANT_RIGHT_SMALLER = 1;
    public static final int TRANSPLANT_GENERAL_CASE = 2;

    public static void transplant(double[] source, int[] indices, int from, int until, double[] target, int targetFrom) {
        for (int i = from; i < until; ++i, ++targetFrom) {
            target[targetFrom] = source[indices[i]];
        }
    }

    private static boolean transplantAndCheckIfNotSmaller(double[] source, int[] indices, int from, int until,
                                                          double[] target, int targetFrom, double threshold) {
        for (int i = from; i < until; ++i, ++targetFrom) {
            double value = source[indices[i]];
            target[targetFrom] = value;
            if (value < threshold) {
                transplant(source, indices, i + 1, until, target, targetFrom + 1);
                return false;
            }
        }
        return true;
    }

    private static boolean transplantAndCheckIfNotGreater(double[] source, int[] indices, int from, int until,
                                                          double[] target, int targetFrom, double threshold) {
        for (int i = from; i < until; ++i, ++targetFrom) {
            double value = source[indices[i]];
            target[targetFrom] = value;
            if (value > threshold) {
                transplant(source, indices, i + 1, until, target, targetFrom + 1);
                return false;
            }
        }
        return true;
    }

    private static boolean transplantAndCheckIfSmaller(double[] source, int[] indices, int from, int until,
                                                          double[] target, int targetFrom, double threshold) {
        for (int i = from; i < until; ++i, ++targetFrom) {
            double value = source[indices[i]];
            target[targetFrom] = value;
            if (value >= threshold) {
                transplant(source, indices, i + 1, until, target, targetFrom + 1);
                return false;
            }
        }
        return true;
    }

    private static boolean transplantAndCheckIfGreater(double[] source, int[] indices, int from, int until,
                                                       double[] target, int targetFrom, double threshold) {
        for (int i = from; i < until; ++i, ++targetFrom) {
            double value = source[indices[i]];
            target[targetFrom] = value;
            if (value <= threshold) {
                transplant(source, indices, i + 1, until, target, targetFrom + 1);
                return false;
            }
        }
        return true;
    }

    private static int transplantAndDecideLeftBased(double[] source, int[] indices,
                                                    int leftFrom, int leftUntil,
                                                    int rightFrom, int rightUntil,
                                                    double[] target, int targetFrom) {
        double leftMin = source[indices[leftFrom]];
        double leftMax = leftMin;
        target[targetFrom] = leftMin;
        ++targetFrom;
        while (++leftFrom < leftUntil) {
            double value = source[indices[leftFrom]];
            if (leftMin > value) {
                leftMin = value;
            }
            if (leftMax < value) {
                leftMax = value;
            }
            target[targetFrom] = value;
            ++targetFrom;
        }
        double right = source[indices[rightFrom]];
        target[targetFrom] = right;
        ++targetFrom;
        ++rightFrom;
        if (leftMax <= right) {
            return transplantAndCheckIfNotSmaller(source, indices, rightFrom, rightUntil, target, targetFrom, leftMax)
                    ? TRANSPLANT_LEFT_NOT_GREATER
                    : TRANSPLANT_GENERAL_CASE;
        } else if (right < leftMin) {
            return transplantAndCheckIfSmaller(source, indices, rightFrom, rightUntil, target, targetFrom, leftMin)
                    ? TRANSPLANT_RIGHT_SMALLER
                    : TRANSPLANT_GENERAL_CASE;
        } else {
            transplant(source, indices, rightFrom, rightUntil, target, targetFrom);
            return TRANSPLANT_GENERAL_CASE;
        }
    }

    private static int transplantAndDecideRightBased(double[] source, int[] indices,
                                                     int leftFrom, int leftUntil,
                                                     int rightFrom, int rightUntil,
                                                     double[] target, int targetFrom) {
        double rightMin = source[indices[rightFrom]];
        double rightMax = rightMin;
        target[targetFrom] = rightMin;
        ++targetFrom;
        while (++rightFrom < rightUntil) {
            double value = source[indices[rightFrom]];
            if (rightMin > value) {
                rightMin = value;
            }
            if (rightMax < value) {
                rightMax = value;
            }
            target[targetFrom] = value;
            ++targetFrom;
        }
        double left = source[indices[leftFrom]];
        target[targetFrom] = left;
        ++targetFrom;
        ++leftFrom;
        if (left > rightMax) {
            return transplantAndCheckIfGreater(source, indices, leftFrom, leftUntil, target, targetFrom, rightMax)
                    ? TRANSPLANT_RIGHT_SMALLER
                    : TRANSPLANT_GENERAL_CASE;
        } else if (rightMin >= left) {
            return transplantAndCheckIfNotGreater(source, indices, leftFrom, leftUntil, target, targetFrom, rightMin)
                    ? TRANSPLANT_LEFT_NOT_GREATER
                    : TRANSPLANT_GENERAL_CASE;
        } else {
            transplant(source, indices, leftFrom, leftUntil, target, targetFrom);
            return TRANSPLANT_GENERAL_CASE;
        }
    }

    public static int transplantAndDecide(double[] source, int[] indices,
                                          int leftFrom, int leftUntil,
                                          int rightFrom, int rightUntil,
                                          double[] target, int targetFrom) {
        if (leftUntil - leftFrom < rightUntil - rightFrom) {
            return transplantAndDecideLeftBased(source, indices,
                    leftFrom, leftUntil, rightFrom, rightUntil, target, targetFrom);
        } else {
            return transplantAndDecideRightBased(source, indices,
                    leftFrom, leftUntil, rightFrom, rightUntil, target, targetFrom);
        }
    }

    public static boolean transplantAndCheckIfSame(double[] source, int[] indices,
                                                   int fromIndex, int untilIndex, double[] target, int targetFrom) {
        double leftFirst = source[indices[fromIndex]];
        target[targetFrom] = leftFirst;
        ++targetFrom;
        while (++fromIndex < untilIndex) {
            double value = source[indices[fromIndex]];
            target[targetFrom] = value;
            ++targetFrom;
            if (value != leftFirst) {
                transplant(source, indices, fromIndex + 1, untilIndex, target, targetFrom);
                return false;
            }
        }
        return true;
    }

    private static double transplantMin(double[] source, int[] indices,
                                        int fromIndex, int untilIndex, double[] target, int targetFrom, double rv) {
        for (int i = fromIndex; i < untilIndex; ++i, ++targetFrom) {
            double v = source[indices[i]];
            if (rv > v) {
                rv = v;
            }
            target[targetFrom] = v;
        }
        return rv;
    }

    public static double transplantAndReturnMinIfNotSameElseNaN(double[] source, int[] indices,
                                                                int fromIndex, int untilIndex,
                                                                double[] target, int targetFrom) {
        double min = source[indices[fromIndex]];
        target[targetFrom] = min;
        ++targetFrom;
        while (++fromIndex < untilIndex) {
            double value = source[indices[fromIndex]];
            target[targetFrom] = value;
            ++targetFrom;
            if (value != min) {
                return transplantMin(source, indices, fromIndex + 1, untilIndex,
                        target, targetFrom, Math.min(value, min));
            }
        }
        return Double.NaN;
    }

    public static int findWhereNotSmaller(int[] indices, int from, int until, int threshold) {
        while (from < until && indices[from] < threshold) {
            ++from;
        }
        return from;
    }

    public static int findLastWhereNotGreater(int[] indices, int from, int until, int threshold) {
        //noinspection StatementWithEmptyBody
        while (from < until && indices[--until] > threshold);
        return until + 1;
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
