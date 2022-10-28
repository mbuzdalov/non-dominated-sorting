package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.median.DestructiveMedianFactory;
import ru.ifmo.nds.util.median.HoareBidirectionalScanV1a;

public class HoareBidirectionalScanV1aTest extends MedianTestsBase {
    @Override
    protected DestructiveMedianFactory getFactory() {
        return HoareBidirectionalScanV1a.instance();
    }
}
