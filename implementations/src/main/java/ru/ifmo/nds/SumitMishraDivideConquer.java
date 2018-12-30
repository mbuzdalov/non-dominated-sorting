package ru.ifmo.nds;

import ru.ifmo.nds.dcns.DCNS_BS;
import ru.ifmo.nds.dcns.DCNS_SS;

public final class SumitMishraDivideConquer {
    private SumitMishraDivideConquer() {}

    private static final NonDominatedSortingFactory DCNS_SS_INSTANCE = DCNS_SS::new;
    private static final NonDominatedSortingFactory DCNS_BS_INSTANCE = DCNS_BS::new;

    public static NonDominatedSortingFactory getDCNS_SS() {
        return DCNS_SS_INSTANCE;
    }

    public static NonDominatedSortingFactory getDCNS_BS() {
        return DCNS_BS_INSTANCE;
    }
}
