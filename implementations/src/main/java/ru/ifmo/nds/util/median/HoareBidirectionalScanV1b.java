package ru.ifmo.nds.util.median;

public final class HoareBidirectionalScanV1b implements DestructiveMedianFactory {
    private static final HoareBidirectionalScanV1b factoryInstance = new HoareBidirectionalScanV1b();

    public static HoareBidirectionalScanV1b instance() {
        return factoryInstance;
    }

    private static int scanImpl(double[] array, double pivot, int from, int index, int to) {
        int l = from, r = to;
        do {
            double tmp = array[l];
            array[l] = array[r];
            array[r] = tmp;
            //noinspection StatementWithEmptyBody
            while (array[++l] < pivot);
            //noinspection StatementWithEmptyBody
            while (array[--r] > pivot);
        } while (l < r);

        if (l == r) {
            ++l;
            --r;
        }

        if (index <= r) {
            return index - r - 1;
        }
        if (index >= l) {
            return index - l + 1;
        }
        return 0;
    }

    private static double solveImpl(double[] array, int from, int until) {
        int index = (from + until) >>> 1;
        int to = until - 1;
        while (to - from >= 3) {
            double pivot = Common.rearrangeReverse3(array, from, index, to);
            int result = scanImpl(array, pivot, from, index, to);

            if (result < -1) {
                to = index - result - 1;
            } else if (result > 1) {
                from = index - result + 1;
            } else if (result == -1) {
                return Common.maxUnchecked(array, from, index);
            } else if (result == 1) {
                return Common.minUnchecked(array, index, to);
            } else {
                return pivot;
            }
        }

        return Common.solve3(array, from);
    }

    private static final DestructiveMedianAlgorithm algorithmInstance = new DestructiveMedianAlgorithm() {
        @Override
        public int maximumSize() {
            return Integer.MAX_VALUE;
        }

        @Override
        public double solve(double[] array, int from, int until) {
            switch (until - from) {
                case 1:
                    return array[from];
                case 2:
                    return Math.max(array[from], array[from + 1]);
                default:
                    return solveImpl(array, from, until);
            }
        }
    };

    @Override
    public DestructiveMedianAlgorithm createInstance(int maxSize) {
        return algorithmInstance;
    }
}
