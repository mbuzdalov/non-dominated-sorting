package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.median.DestructiveMedianFactory;
import ru.ifmo.nds.util.median.HoareBidirectionalScanV1;

public class HoareBidirectionalScanV1Test extends MedianTestsBase {
    @Override
    protected DestructiveMedianFactory getFactory() {
        return HoareBidirectionalScanV1.instance();
    }
}
