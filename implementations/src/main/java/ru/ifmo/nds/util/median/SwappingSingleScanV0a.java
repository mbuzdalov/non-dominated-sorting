package ru.ifmo.nds.util.median;

public final class SwappingSingleScanV0a implements DestructiveMedianAlgorithm {
    private final double[] extraSpace;

    private SwappingSingleScanV0a(int maxSize) {
        extraSpace = new double[maxSize];
    }

    @Override
    public int maximumSize() {
        return extraSpace.length;
    }

    @Override
    public double solve(double[] array, int from, int until) {
        double[] temp = extraSpace;
        switch (until - from) {
            case 1:
                return array[from];
            case 2:
                return Math.max(array[from], array[from + 1]);
        }

        int to = until - 1;
        int resultIndex = (from + until) >>> 1;
        while (to - from >= 3) {
            int midIndex = (from + to) >>> 1;
            double pivot = Common.rearrange3(array, from, midIndex, to, temp);
            int leftTo = from, rightFrom = to;
            for (int i = from + 1; i < to; ++i) {
                double v = array[i];
                temp[v <= pivot ? ++leftTo : --rightFrom] = v;
            }
            double[] swp = temp; temp = array; array = swp;

            if (resultIndex < leftTo) {
                to = leftTo;
            } else if (resultIndex > rightFrom) {
                from = rightFrom;
            } else if (resultIndex == leftTo) {
                return Common.maxUnchecked(array, from, leftTo);
            } else {
                return Common.minUnchecked(array, rightFrom, to);
            }
        }

        return Common.solve3(array, from);
    }

    private static final DestructiveMedianFactory factory = SwappingSingleScanV0a::new;

    public static DestructiveMedianFactory factory() {
        return factory;
    }
}
