package ru.ifmo.nds;

import ru.ifmo.nds.dcns.AlternativeImplementation;
import ru.ifmo.nds.dcns.ImprovedSumitImplementation;

public final class SumitMishraDivideConquer {
    private SumitMishraDivideConquer() {}

    public static NonDominatedSortingFactory getAlternativeImplementation(boolean useBinarySearch) {
        return (p, d) -> new AlternativeImplementation(p, d, useBinarySearch);
    }

    public static NonDominatedSortingFactory getSumitImplementation(boolean useBinarySearch, boolean useGammaHeuristic) {
        return (p, d) -> new ImprovedSumitImplementation(p, d, useBinarySearch, useGammaHeuristic);
    }
}
