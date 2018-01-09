package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.ArrayHelper;

public class MedianTests_CentralQuickSort extends MedianTestsBase {
    @Override
    protected double destructiveMedian(double[] array, int until) {
        return ArrayHelper.destructiveMedianCenter(array, 0, until);
    }
}
