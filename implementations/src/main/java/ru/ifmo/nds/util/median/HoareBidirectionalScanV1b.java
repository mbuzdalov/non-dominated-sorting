package ru.ifmo.nds.util.median;

public final class HoareBidirectionalScanV1b implements DestructiveMedianFactory {
    private static final HoareBidirectionalScanV1b factoryInstance = new HoareBidirectionalScanV1b();
    private static final int HEAP_CONSTANT = 4;

    public static HoareBidirectionalScanV1b instance() {
        return factoryInstance;
    }

    static final class ScanResult {
        final int l, r;

        ScanResult(int l, int r) {
            this.l = l;
            this.r = r;
        }
    }

    static ScanResult scanImpl(double[] array, double pivot, int from, int to) {
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

        return new ScanResult(l, r);
    }


    static double solveImpl(double[] array, int from, int until) {
        int index = (from + until) >>> 1;
        int to = until - 1;
        while (true) {
            double pivot = Common.rearrangeReverse3(array, from, index, to);
            ScanResult result = scanImpl(array, pivot, from, to);

            if (index < result.r - HEAP_CONSTANT) {
                to = result.r;
            } else if (result.l + HEAP_CONSTANT < index) {
                from = result.l;
            } else if (index <= result.r) {
                return Common.kthMaxHeap(array, from, result.r, result.r - index);
            } else if (result.l <= index) {
                return Common.kthMinHeap(array, result.l, to, index - result.l);
            } else {
                return pivot;
            }
        }
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
