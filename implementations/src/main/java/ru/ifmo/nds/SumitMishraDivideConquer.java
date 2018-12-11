package ru.ifmo.nds;

import ru.ifmo.nds.dcns.ImprovedSumitImplementation;

public final class SumitMishraDivideConquer {
    private SumitMishraDivideConquer() {}

    public static NonDominatedSortingFactory getSumitImplementation(boolean useBinarySearch, boolean useGammaHeuristic) {
        return (p, d) -> new ImprovedSumitImplementation(p, d, useBinarySearch, useGammaHeuristic);
    }
}
