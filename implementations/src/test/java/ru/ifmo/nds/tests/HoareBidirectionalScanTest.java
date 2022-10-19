package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.median.DestructiveMedianFactory;
import ru.ifmo.nds.util.median.HoareBidirectionalScan;

public class HoareBidirectionalScanTest extends MedianTestsBase {
    @Override
    protected DestructiveMedianFactory getFactory() {
        return HoareBidirectionalScan.instance();
    }
}
