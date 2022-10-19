package ru.ifmo.nds.util.median;

final class Common {
    private Common() {}

    static double minUnchecked(double[] array, int fromInc, int toInc) {
        double result = array[fromInc];
        while (++fromInc <= toInc) {
            result = Math.min(result, array[fromInc]);
        }
        return result;
    }

    static double maxUnchecked(double[] array, int fromInc, int toInc) {
        double result = array[fromInc];
        while (++fromInc <= toInc) {
            result = Math.max(result, array[fromInc]);
        }
        return result;
    }
}
