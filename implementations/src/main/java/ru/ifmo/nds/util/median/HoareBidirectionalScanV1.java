package ru.ifmo.nds.util.median;

public final class HoareBidirectionalScanV1 implements DestructiveMedianFactory {
    private static final HoareBidirectionalScanV1 factoryInstance = new HoareBidirectionalScanV1();

    public static HoareBidirectionalScanV1 instance() {
        return factoryInstance;
    }

    private static final DestructiveMedianAlgorithm algorithmInstance = new DestructiveMedianAlgorithm() {
        @Override
        public int maximumSize() {
            return Integer.MAX_VALUE;
        }

        @Override
        public double solve(double[] array, int from, int until) {
            int to = until - 1;
            if (from == to) {
                return array[from];
            }
            int index = (from + until) >>> 1;

            while (true) {
                double pivot = array[(from + to) >>> 1];
                if (from + 4 < to) {
                    double mid = (array[from] + array[to]) / 2;
                    pivot = (pivot + mid) / 2;
                }
                double vl, vr;
                int l = from, r = to;
                do {
                    while ((vl = array[l]) < pivot) ++l;
                    while ((vr = array[r]) > pivot) --r;
                    if (l <= r) {
                        array[l] = vr;
                        array[r] = vl;
                        ++l;
                        --r;
                    }
                } while (l <= r);
                if (index < r) {
                    to = r;
                } else if (l < index) {
                    from = l;
                } else if (r == index) {
                    return Common.maxUnchecked(array, from, r);
                } else if (l == index) {
                    return Common.minUnchecked(array, l, to);
                } else {
                    return pivot;
                }
            }
        }
    };

    @Override
    public DestructiveMedianAlgorithm createInstance(int maxSize) {
        return algorithmInstance;
    }
}
