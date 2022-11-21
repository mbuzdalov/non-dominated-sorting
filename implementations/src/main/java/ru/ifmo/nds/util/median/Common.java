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

    static double rearrangeReverse3(double[] array, int a, int b, int c) {
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
        array[a] = v2;
        array[b] = v1;
        array[c] = v0;
        return v1;
    }

    private static void minSiftDown(double[] array, int index, int to, int offset) {
        double value = array[index];
        while (true) {
            int c1 = (index << 1) - offset;
            if (c1 > to) {
                break;
            }
            double v1 = array[c1];
            int c2 = c1 + 1;
            if (c2 <= to && array[c2] < v1) {
                c1 = c2;
                v1 = array[c2];
            }
            if (v1 < value) {
                array[index] = v1;
                array[c1] = value;
                index = c1;
            } else {
                break;
            }
        }
    }

    private static void maxSiftDown(double[] array, int index, int to, int offset) {
        double value = array[index];
        while (true) {
            int c1 = (index << 1) - offset;
            if (c1 > to) {
                break;
            }
            double v1 = array[c1];
            int c2 = c1 + 1;
            if (c2 <= to && array[c2] > v1) {
                c1 = c2;
                v1 = array[c2];
            }
            if (v1 > value) {
                array[index] = v1;
                array[c1] = value;
                index = c1;
            } else {
                break;
            }
        }
    }

    static double kthMinHeap(double[] array, int from, int to, int k) {
        if (k == 0) {
            return minUnchecked(array, from, to);
        }

        int offset = from - 1;
        // 1: Make heap
        for (int i = (to + offset) >>> 1; i >= from; --i) {
            minSiftDown(array, i, to, offset);
        }
        // 2: Drop minima
        while (k > 0) {
            array[from] = array[to];
            minSiftDown(array, from, --to, offset);
            --k;
        }
        return array[from];
    }

    static double kthMaxHeap(double[] array, int from, int to, int k) {
        if (k == 0) {
            return maxUnchecked(array, from, to);
        }

        int offset = from - 1;
        // 1: Make heap
        for (int i = (to + offset) >>> 1; i >= from; --i) {
            maxSiftDown(array, i, to, offset);
        }
        // 2: Drop minima
        while (k > 0) {
            array[from] = array[to];
            maxSiftDown(array, from, --to, offset);
            --k;
        }
        return array[from];
    }
}
