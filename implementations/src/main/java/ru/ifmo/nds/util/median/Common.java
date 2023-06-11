package ru.ifmo.nds.util.median;

final class Common {
    private Common() {}

    static double minUnchecked(double[] array, int fromInc, int toInc) {
        double result = array[fromInc];
        while (++fromInc <= toInc) {
            double value = array[fromInc];
            if (result > value) {
                result = value;
            }
        }
        return result;
    }

    static double minUncheckedMove(double[] array, int fromInc, int toInc) {
        int minIndex = fromInc;
        double result = array[fromInc];
        for (int i = fromInc; ++i <= toInc; ) {
            double value = array[i];
            if (result > value) {
                result = value;
                minIndex = i;
            }
        }
        if (minIndex != fromInc) {
            array[minIndex] = array[fromInc];
            array[fromInc] = result;
        }
        return result;
    }

    static double maxUnchecked(double[] array, int fromInc, int toInc) {
        double result = array[fromInc];
        while (++fromInc <= toInc) {
            double value = array[fromInc];
            if (result < value) {
                result = value;
            }
        }
        return result;
    }

    static double maxUncheckedMove(double[] array, int fromInc, int toInc) {
        int maxIndex = fromInc;
        double result = array[fromInc];
        for (int i = fromInc; ++i <= toInc; ) {
            double value = array[i];
            if (result < value) {
                result = value;
                maxIndex = i;
            }
        }
        if (maxIndex != toInc) {
            array[maxIndex] = array[toInc];
            array[toInc] = result;
        }
        return result;
    }

    static double solve3(double[] array, int offset) {
        double v0 = array[offset];
        double v1 = array[offset + 1];
        double v2 = array[offset + 2];
        if (v0 <= v1) {
            if (v1 <= v2) {
                return v1;
            } else {
                return Math.max(v0, v2);
            }
        } else {
            if (v0 <= v2) {
                return v0;
            } else {
                return Math.max(v1, v2);
            }
        }
    }

    static double rearrange3(double[] array, int a, int b, int c, double[] temp) {
        double v0 = array[a];
        double v1 = array[b];
        double v2 = array[c];
        if (v0 > v1) {
            double tmp = v0; v0 = v1; v1 = tmp;
        }
        if (v1 > v2) {
            double tmp = v1; v1 = v2; v2 = tmp;
        }
        if (v0 > v1) {
            double tmp = v0; v0 = v1; v1 = tmp;
        }
        temp[a] = v0;
        array[b] = v1;
        temp[c] = v2;
        return v1;
    }

    static double rearrange3(double[] array, int a, int b, int c) {
        double v0 = array[a];
        double v1 = array[b];
        double v2 = array[c];
        if (v0 > v1) {
            double tmp = v0; v0 = v1; v1 = tmp;
        }
        if (v1 > v2) {
            double tmp = v1; v1 = v2; v2 = tmp;
        }
        if (v0 > v1) {
            double tmp = v0; v0 = v1; v1 = tmp;
        }
        array[a] = v0;
        array[b] = v1;
        array[c] = v2;
        return v1;
    }
}
