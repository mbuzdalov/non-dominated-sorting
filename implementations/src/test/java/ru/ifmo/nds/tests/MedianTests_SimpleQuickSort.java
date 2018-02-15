package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.ArrayHelper;

public class MedianTests_SimpleQuickSort extends MedianTestsBase {
    @Override
    protected double destructiveMedian(double[] array, int until) {
        return ArrayHelper.destructiveMedianSimple(array, 0, until);
    }
}
