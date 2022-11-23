package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.median.DestructiveMedianFactory;
import ru.ifmo.nds.util.median.SingleScanV1;

public class SingleScanV1Test extends MedianTestsBase {
    @Override
    protected DestructiveMedianFactory getFactory() {
        return SingleScanV1.factory();
    }
}
