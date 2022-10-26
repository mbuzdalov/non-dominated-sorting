package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.median.DestructiveMedianFactory;
import ru.ifmo.nds.util.median.SwappingSingleScanV1a;

public class SwappingSingleScanV1aTest extends MedianTestsBase {
    @Override
    protected DestructiveMedianFactory getFactory() {
        return SwappingSingleScanV1a.instance();
    }
}
