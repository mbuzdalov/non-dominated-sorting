package ru.ifmo.nds;

import ru.ifmo.nds.dcns.AlternativeImplementation;

public final class SumitMishraDivideConquer {
    private SumitMishraDivideConquer() {}

    public static NonDominatedSortingFactory getAlternativeImplementation(boolean useBinarySearch) {
        return (p, d) -> new AlternativeImplementation(p, d, useBinarySearch);
    }
}
