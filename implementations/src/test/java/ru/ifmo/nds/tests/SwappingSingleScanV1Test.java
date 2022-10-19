package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.median.DestructiveMedianFactory;
import ru.ifmo.nds.util.median.SwappingSingleScanV1;

public class SwappingSingleScanV1Test extends MedianTestsBase {
    @Override
    protected DestructiveMedianFactory getFactory() {
        return SwappingSingleScanV1.instance();
    }
}
