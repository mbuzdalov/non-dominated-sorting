package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.median.DestructiveMedianFactory;
import ru.ifmo.nds.util.median.HoareBidirectionalScanV1b;

public class HoareBidirectionalScanV1bTest extends MedianTestsBase {
    @Override
    protected DestructiveMedianFactory getFactory() {
        return HoareBidirectionalScanV1b.instance();
    }
}
