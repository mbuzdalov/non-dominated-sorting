package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.median.DestructiveMedianFactory;
import ru.ifmo.nds.util.median.HoareBidirectionalScanV0;

public class HoareBidirectionalScanV0Test extends MedianTestsBase {
    @Override
    protected DestructiveMedianFactory getFactory() {
        return HoareBidirectionalScanV0.instance();
    }
}
