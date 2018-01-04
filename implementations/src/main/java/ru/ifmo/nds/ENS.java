package ru.ifmo.nds;

import ru.ifmo.nds.ens.ENS_BS;
import ru.ifmo.nds.ens.ENS_SS;
import ru.ifmo.nds.ndt.ENS_NDT;
import ru.ifmo.nds.ndt.ENS_NDT_Arrays;

public class ENS {
    private ENS() {}

    private static final NonDominatedSortingFactory ENS_SS_INSTANCE = ENS_SS::new;
    private static final NonDominatedSortingFactory ENS_BS_INSTANCE = ENS_BS::new;
    private static final NonDominatedSortingFactory ENS_NDT_INSTANCE = ENS_NDT::new;
    private static final NonDominatedSortingFactory ENS_NDT_ARRAYS_INSTANCE = ENS_NDT_Arrays::new;

    public static NonDominatedSortingFactory getENS_SS() {
        return ENS_SS_INSTANCE;
    }

    public static NonDominatedSortingFactory getENS_BS() {
        return ENS_BS_INSTANCE;
    }

    public static NonDominatedSortingFactory getENS_NDT() {
        return ENS_NDT_INSTANCE;
    }

    public static NonDominatedSortingFactory getENS_NDT_Arrays() {
        return ENS_NDT_ARRAYS_INSTANCE;
    }
}
