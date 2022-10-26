package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.median.DestructiveMedianFactory;
import ru.ifmo.nds.util.median.SwappingSingleScanV0a;

public class SwappingSingleScanV0aTest extends MedianTestsBase {
    @Override
    protected DestructiveMedianFactory getFactory() {
        return SwappingSingleScanV0a.instance();
    }
}
