package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.ArrayHelper;

public class MedianTests_SimpleQuickSort3 extends MedianTestsBase {
    @Override
    protected double destructiveMedian(double[] array, int until) {
        return ArrayHelper.destructiveMedian3(array, 0, until);
    }
}
