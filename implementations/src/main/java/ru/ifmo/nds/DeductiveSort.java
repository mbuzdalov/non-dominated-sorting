package ru.ifmo.nds;

import ru.ifmo.nds.deductive.*;

public final class DeductiveSort {
    private DeductiveSort() {}

    private static final NonDominatedSortingFactory INSTANCE_V0 = Original::new;
    private static final NonDominatedSortingFactory INSTANCE_V1 = LibraryV1::new;
    private static final NonDominatedSortingFactory INSTANCE_V2 = LibraryV2::new;
    private static final NonDominatedSortingFactory INSTANCE_V3 = LibraryV3::new;
    private static final NonDominatedSortingFactory INSTANCE_V4 = LibraryV4::new;
    private static final NonDominatedSortingFactory INSTANCE_V5 = LibraryV5::new;
    private static final NonDominatedSortingFactory INSTANCE_V6 = LibraryV6::new;
    private static final NonDominatedSortingFactory INSTANCE_QF = QuadraticFast::new;

    public static NonDominatedSortingFactory getOriginalImplementation() {
        return INSTANCE_V0;
    }

    public static NonDominatedSortingFactory getLibraryImplementationV1() {
        return INSTANCE_V1;
    }

    public static NonDominatedSortingFactory getLibraryImplementationV2() {
        return INSTANCE_V2;
    }

    public static NonDominatedSortingFactory getLibraryImplementationV3() {
        return INSTANCE_V3;
    }

    public static NonDominatedSortingFactory getLibraryImplementationV4() {
        return INSTANCE_V4;
    }

    public static NonDominatedSortingFactory getLibraryImplementationV5() {
        return INSTANCE_V5;
    }

    public static NonDominatedSortingFactory getLibraryImplementationV6() {
        return INSTANCE_V6;
    }

    public static NonDominatedSortingFactory getQuadraticFastImplementation() {
        return INSTANCE_QF;
    }
}
