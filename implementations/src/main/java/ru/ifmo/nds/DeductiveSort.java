package ru.ifmo.nds;

import ru.ifmo.nds.deductive.LibraryV1;
import ru.ifmo.nds.deductive.LibraryV2;
import ru.ifmo.nds.deductive.Original;

public final class DeductiveSort {
    private DeductiveSort() {}

    private static final NonDominatedSortingFactory INSTANCE_V0 = Original::new;
    private static final NonDominatedSortingFactory INSTANCE_V1 = LibraryV1::new;
    private static final NonDominatedSortingFactory INSTANCE_V2 = LibraryV2::new;

    public static NonDominatedSortingFactory getOriginalImplementation() {
        return INSTANCE_V0;
    }

    public static NonDominatedSortingFactory getLibraryImplementationV1() {
        return INSTANCE_V1;
    }

    public static NonDominatedSortingFactory getLibraryImplementationV2() {
        return INSTANCE_V2;
    }
}
