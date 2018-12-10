package ru.ifmo.nds.util;

public final class MathEx {
    private MathEx() {}

    public static int log2up(int n) {
        if (n <= 0) {
            throw new IllegalArgumentException("log2up is undefined for non-positive values");
        }
        return 32 - Integer.numberOfLeadingZeros(n - 1);
    }
}
