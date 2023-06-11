package ru.ifmo.nds.tests;

import ru.ifmo.nds.util.median.DestructiveMedianFactory;
import ru.ifmo.nds.util.median.SubsamplingV0;

public class SubsamplingV0Test extends MedianTestsBase {
    @Override
    protected DestructiveMedianFactory getFactory() {
        return SubsamplingV0.factory();
    }
}
