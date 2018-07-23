package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.ArrayHelper;

public class MedianQuickSelectTest extends MedianTestsBase {
    @Override
    protected double destructiveMedian(double[] array, int until) {
        return ArrayHelper.destructiveMedian(array, 0, until);
    }
}
