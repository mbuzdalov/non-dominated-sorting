package ru.ifmo.nds.util.median;

public final class SingleScanV1 implements DestructiveMedianFactory {
    private static final SingleScanV1 factory = new SingleScanV1();

    public static SingleScanV1 factory() {
        return factory;
    }

    static int findRightStart(double[] array, int from, double pivot) {
        //noinspection StatementWithEmptyBody
        while (array[++from] < pivot);
        return from;
    }

    static int scanRemaining(double[] array, int rightStart, double pivot, int to) {
        int curr = rightStart;
        while (++curr < to) {
            double value = array[curr];
            if (value < pivot) {
                array[curr] = array[rightStart];
                array[rightStart] = value;
                ++rightStart;
            }
        }
        return rightStart;
    }

    static double solveImpl(double[] array, int from, int until) {
        int index = (from + until) >>> 1;
        int to = until - 1;

        while (to - from >= 3) {
            double pivot = Common.rearrange3(array, from, index, to);
            int rightStart = findRightStart(array, from, pivot);
            rightStart = scanRemaining(array, rightStart, pivot, to);

            int leftEnd = rightStart - 1;
            if (index < leftEnd) {
                to = leftEnd;
            } else if (index > rightStart) {
                from = rightStart;
            } else if (index == leftEnd) {
                return Common.maxUnchecked(array, from, leftEnd);
            } else {
                return Common.minUnchecked(array, rightStart, to);
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
