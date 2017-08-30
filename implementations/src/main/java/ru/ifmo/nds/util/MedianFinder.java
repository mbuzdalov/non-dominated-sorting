package ru.ifmo.nds.util;

import java.util.concurrent.ThreadLocalRandom;

public final class MedianFinder {
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    private double[] medianSwap;
    private int medianCount = 0, countLarger = -1, countSmaller = -1;
    private double lastMedianRunMin, lastMedianRunMax;

    public MedianFinder(int maximumPoints) {
        this.medianSwap = new double[maximumPoints];
    }

    public void consumeDataForMedian(double[] points, int[] indices, int from, int until) {
        lastMedianRunMax = Double.NEGATIVE_INFINITY;
        lastMedianRunMin = Double.POSITIVE_INFINITY;
        for (int i = from; i < until; ++i) {
            double v = points[indices[i]];
            lastMedianRunMin = Math.min(lastMedianRunMin, v);
            lastMedianRunMax = Math.max(lastMedianRunMax, v);
            medianSwap[medianCount++] = v;
        }
    }

    public double getLastMedianConsumptionMax() {
        return lastMedianRunMax;
    }

    public double getLastMedianConsumptionMin() {
        return lastMedianRunMin;
    }

    public void resetMedian() {
        medianCount = 0;
    }

    private double findMedianGeneralCase() {
        medianImpl(medianCount, medianCount / 2);
        double rv = medianSwap[medianCount / 2];
        for (int i = 0; i < medianCount; ++i) {
            if (medianSwap[i] < rv) {
                ++countSmaller;
            } else if (medianSwap[i] > rv) {
                ++countLarger;
            }
        }
        return rv;
    }

    private double median2() {
        double a = medianSwap[0], b = medianSwap[1];
        if (a != b) {
            ++countSmaller;
        }
        return Math.max(a, b);
    }

    private double median3() {
        double a = medianSwap[0], b = medianSwap[1], c = medianSwap[2];
        if (b > c) {
            double tmp = b;
            b = c;
            c = tmp;
            if (a <= b) {
                if (a < b) {
                    ++countSmaller;
                }
                ++countLarger;
                return b;
            } else if (a <= c) {
                ++countSmaller;
                if (a < c) {
                    ++countLarger;
                }
                return a;
            } else {
                ++countLarger;
                ++countSmaller;
                return c;
            }
        } else {
            if (a <= b) {
                if (a < b) {
                    ++countSmaller;
                }
                if (b < c) {
                    ++countLarger;
                }
                return b;
            } else if (a <= c) {
                ++countSmaller;
                if (a < c) {
                    ++countLarger;
                }
                return a;
            } else {
                ++countLarger;
                if (b < c) {
                    ++countSmaller;
                }
                return c;
            }
        }
    }

    public double findMedian() {
        countLarger = countSmaller = 0;
        switch (medianCount) {
            case 1: return medianSwap[0];
            case 2: return median2();
            case 3: return median3();
            default: return findMedianGeneralCase();
        }
    }

    private void medianImpl(int until, int index) {
        int from = 0;
        int count = 0;
        while (from + 1 < until) {
            double pivot = medianSwap[++count > 20 ? random.nextInt(from, until) : (from + until) >>> 1];
            int l = from, r = until - 1;
            while (l <= r) {
                while (medianSwap[l] < pivot) ++l;
                while (medianSwap[r] > pivot) --r;
                if (l <= r) {
                    ArrayHelper.swap(medianSwap, l++, r--);
                }
            }
            if (index <= r) {
                until = r + 1;
            } else if (l <= index) {
                from = l;
            } else {
                break;
            }
        }
    }

    public int howManyLargerThanMedian() {
        return countLarger;
    }

    public int howManySmallerThanMedian() {
        return countSmaller;
    }
}
