package ru.ifmo.nds.util;

public final class MedianFinder {
    private double[] medianSwap;
    private int medianCount = 0, countLarger = -1, countSmaller = -1;
    private double lastMedianRunMin, lastMedianRunMax;

    public MedianFinder(int maximumPoints) {
        this.medianSwap = new double[maximumPoints];
    }

    public void consumeDataForMedian(double[] points, int[] indices, int from, int until) {
        int newMedianCount = ArrayHelper.transplant(points, indices, from, until, medianSwap, medianCount);
        lastMedianRunMax = ArrayHelper.max(medianSwap, medianCount, newMedianCount);
        lastMedianRunMin = ArrayHelper.min(medianSwap, medianCount, newMedianCount);
        medianCount = newMedianCount;
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

    public double findMedian() {
        double rv = ArrayHelper.destructiveMedian(medianSwap, 0, medianCount);
        countSmaller = ArrayHelper.countSmaller(medianSwap, 0, medianCount, rv);
        countLarger = ArrayHelper.countGreater(medianSwap, 0, medianCount, rv);
        return rv;
    }

    public int howManyLargerThanMedian() {
        return countLarger;
    }

    public int howManySmallerThanMedian() {
        return countSmaller;
    }
}
