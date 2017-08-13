package ru.ifmo.nds;

import ru.ifmo.nds.dcns.SumitImplementation2016;

public class SumitMishraDivideConquer {
    private SumitMishraDivideConquer() {}

    public static NonDominatedSortingFactory getSumitImplementation2016(boolean useBinarySearch, boolean useGammaHeuristic) {
        return (p, d) -> new SumitImplementation2016(p, d, useBinarySearch, useGammaHeuristic);
    }
}
