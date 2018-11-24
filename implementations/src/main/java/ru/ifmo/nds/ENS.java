package ru.ifmo.nds;

import ru.ifmo.nds.ens.ENS_BS;
import ru.ifmo.nds.ens.ENS_SS;
import ru.ifmo.nds.ens.ENS_HS;
import ru.ifmo.nds.ndt.ENS_NDT;
import ru.ifmo.nds.ndt.ENS_NDT_Arrays;
import ru.ifmo.nds.ndt.ENS_NDT_OneTree;

public final class ENS {
    private ENS() {}

    private static final NonDominatedSortingFactory ENS_SS_INSTANCE = ENS_SS::new;
    private static final NonDominatedSortingFactory ENS_BS_INSTANCE = ENS_BS::new;
    private static final NonDominatedSortingFactory ENS_HS_INSTANCE = ENS_HS::new;
    private static final NonDominatedSortingFactory ENS_NDT_ARRAYS_INSTANCE = ENS_NDT_Arrays::new;

    public static NonDominatedSortingFactory getENS_SS() {
        return ENS_SS_INSTANCE;
    }

    public static NonDominatedSortingFactory getENS_BS() {
        return ENS_BS_INSTANCE;
    }

    public static NonDominatedSortingFactory getENS_HS() {
        return ENS_HS_INSTANCE;
    }

    public static NonDominatedSortingFactory getENS_NDT(int threshold) {
        // currently, the best value for threshold seems to be 8.
        return (int maxPoints, int maxDimension) -> new ENS_NDT(maxPoints, maxDimension, threshold);
    }

    public static NonDominatedSortingFactory getENS_NDT_OneTree(int threshold) {
        return (int maxPoints, int maxDimension) -> new ENS_NDT_OneTree(maxPoints, maxDimension, threshold);
    }

    public static NonDominatedSortingFactory getENS_NDT_Arrays() {
        return ENS_NDT_ARRAYS_INSTANCE;
    }
}
