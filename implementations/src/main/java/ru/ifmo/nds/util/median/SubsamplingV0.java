package ru.ifmo.nds.util.median;

/**
 * This destructive median algorithm is based on the bachelor thesis of Alvaro Fernandez de la Fuente.
 * It samples a small portion of the input array, determines some gap around a median there,
 * and writes out the portion of the input array that is contained within the elements surrounding that gap.
 * If successful, the next phase is likely much smaller (of order n^(2/3) of size, based on the current tune),
 * otherwise, some other algorithm is called, which is supposed to happen with a small probability.
 */
public final class SubsamplingV0 implements DestructiveMedianAlgorithm {
    private final double[] sampleArray;
    private final HoareBidirectionalScanV1 smallSolver = new HoareBidirectionalScanV1();

    private SubsamplingV0(int maxSize) {
        sampleArray = new double[maxSize];
    }

    /* The logic for the next two methods is:
     *
     * Microbenchmark indicates this:
     *         sampleSize / windowSize
     * n = 32: 5/1
     * n = 64: 5/1 and 8/2
     * n = 128: 11/2
     * n = 256: 25/4 (maybe 19/3)
     * n = 512: 36/4 and 44/5
     * n = 1024: 70/7 (maybe 78/8)
     * n = 2048: 102/9 (106/10?)
     *
     * Based on the "power laws" interpolated based on "simulations":
     *   sampleSize = pow(n, 0.75) * 0.32
     *   windowSize = pow(n, 0.42) * 0.4
     */

    private int sampleSize(int currentSize) {
        if (currentSize <= 64) return 5;
        if (currentSize <= 128) {
            if (currentSize <= 76) return 8;
            if (currentSize <= 90) return 9;
            if (currentSize <= 108) return 10;
            return 11;
        }
        return (int) (Math.pow(currentSize, 0.75) * 0.32);
    }

    private int windowSize(int currentSize) {
        if (currentSize <= 64) return 1;
        if (currentSize <= 128) return 2;
        return (int) (Math.pow(currentSize, 0.42) * 0.4);
    }

    @Override
    public int maximumSize() {
        return sampleArray.length;
    }

    @Override
    public double solve(double[] array, int from, int until) {
        int size = until - from;
        if (size <= 27) {
            return smallSolver.solve(array, from, until);
        }

        int index = (from + until) >>> 1;
        // Stage 1: Sampling
        int sampleSize = sampleSize(size);
        int stepSize = (size - 1) / (sampleSize - 1);
        int originalArrayStart = (size - (1 + stepSize * (sampleSize - 1))) / 2;
        int sampleEnd = from;
        for (int i = from + originalArrayStart; i < until; i += stepSize, ++sampleEnd) {
            sampleArray[sampleEnd] = array[i];
        }

        // Stage 2: Finding statistics in the sample
        int windowSize = windowSize(size);
        int minIndex = from + Math.max(0, (sampleSize >>> 1) - windowSize);
        int maxIndex = from + Math.min(sampleSize - 1, (sampleSize >>> 1) + windowSize);
        double minValue = kthHoare(sampleArray, from, sampleEnd, minIndex);
        double maxValue = kthHoare(sampleArray, minIndex, sampleEnd, maxIndex);

        // Here, we stop using sampleArray for subsampling the initial array.
        // In the non-trivial case, we use it again for storing filtered values.

        // Stage 3: Filtering the values between minValue and maxValue,
        // and, if successful, finding the kth order statistics on them.

        if (minValue == maxValue) {
            // Only need to count how many elements of which sort I have
            int offsetLess = from, offsetLessEqual = from;
            for (int i = from; i < until; ++i) {
                double v = array[i];
                offsetLess += v < minValue ? 1 : 0;
                offsetLessEqual += v <= minValue ? 1 : 0;
            }
            if (index >= offsetLess && index < offsetLessEqual) {
                return minValue;
            } else {
                return kthHoare(array, from, until, index);
            }
        } else {
            // Need to actually sub-sample
            int countLess = 0, offsetSubSample = from;
            for (int i = from; i < until; ++i) {
                double v = array[i];
                int ifLessThen1 = v < minValue ? 1 : 0;
                countLess += ifLessThen1;
                sampleArray[offsetSubSample] = v;
                offsetSubSample += (v <= maxValue ? 1 : 0) - ifLessThen1;
            }
            int newIndex = index - countLess;
            if (newIndex >= from && newIndex < offsetSubSample) {
                return kthHoare(sampleArray, from, offsetSubSample, newIndex);
            } else {
                return kthHoare(array, from, until, index);
            }
        }
    }

    private static final DestructiveMedianFactory factory = SubsamplingV0::new;
    public static DestructiveMedianFactory factory() {
        return factory;
    }

    // Will be there until I refactor the whole thing to solve kth order in the general case.
    // This is a non-destructive median, in terms that the array remains a permutation of the original array.
    private static double kthHoare(double[] array, int from, int until, int index) {
        int to = until - 1;
        if (to - from == 0) {
            return array[from];
        } else {
            if (index == from) return Common.minUncheckedMove(array, from, to);
            if (index == to) return Common.maxUncheckedMove(array, from, to);
        }

        while (to - from >= 3) {
            double pivot = Common.rearrange3(array, from, index, to);

            double vl, vr;
            int l = from + 1, r = to - 1;
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
                return Common.maxUncheckedMove(array, from, r);
            } else if (l == index) {
                return Common.minUncheckedMove(array, l, to);
            } else {
                return pivot;
            }
        }

        return Common.rearrange3(array, from, from + 1, to);
    }
}
