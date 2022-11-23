package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.median.DestructiveMedianFactory;
import ru.ifmo.nds.util.median.SwappingSingleScanV0;

public class SwappingSingleScanV0Test extends MedianTestsBase {
    @Override
    protected DestructiveMedianFactory getFactory() {
        return SwappingSingleScanV0.factory();
    }
}
