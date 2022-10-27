package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.median.DestructiveMedianFactory;
import ru.ifmo.nds.util.median.SwappingSingleScanV1b;

public class SwappingSingleScanV1bTest extends MedianTestsBase {
    @Override
    protected DestructiveMedianFactory getFactory() {
        return SwappingSingleScanV1b.instance();
    }
}
