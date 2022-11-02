package ru.ifmo.nds.util.median;

public final class HoareBidirectionalScanV0 implements DestructiveMedianFactory {
    private static final HoareBidirectionalScanV0 factoryInstance = new HoareBidirectionalScanV0();

    public static HoareBidirectionalScanV0 instance() {
        return factoryInstance;
    }

    private static final DestructiveMedianAlgorithm algorithmInstance = new DestructiveMedianAlgorithm() {
        @Override
        public int maximumSize() {
            return Integer.MAX_VALUE;
        }

        @Override
        public double solve(double[] array, int from, int until) {
            int index = (from + until) >>> 1;
            int to = until - 1;
            while (from < to) {
                double pivot = array[(from + to) >>> 1];
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
                if (index <= r) {
                    to = r;
                } else if (l <= index) {
                    from = l;
                } else {
                    break;
                }
            }

            return array[index];
        }
    };

    @Override
    public DestructiveMedianAlgorithm createInstance(int maxSize) {
        return algorithmInstance;
    }
}
