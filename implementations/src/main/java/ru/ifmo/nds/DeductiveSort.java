package ru.ifmo.nds;

import ru.ifmo.nds.deductive.*;

public final class DeductiveSort {
    private DeductiveSort() {}

    private static final NonDominatedSortingFactory INSTANCE_O1 = OriginalV1::new;
    private static final NonDominatedSortingFactory INSTANCE_R1 = ReorderingV1::new;
    private static final NonDominatedSortingFactory INSTANCE_R2 = ReorderingV2::new;
    private static final NonDominatedSortingFactory INSTANCE_RQ1 = RandomizedQuadraticV1::new;
    private static final NonDominatedSortingFactory INSTANCE_RQ2 = RandomizedQuadraticV2::new;
    private static final NonDominatedSortingFactory INSTANCE_DQ1 = DeterministicQuadraticV1::new;
    private static final NonDominatedSortingFactory INSTANCE_DQ2 = DeterministicQuadraticV2::new;
    private static final NonDominatedSortingFactory INSTANCE_DQ3 = DeterministicQuadraticV3::new;
    private static final NonDominatedSortingFactory INSTANCE_DQ4 = DeterministicQuadraticV4::new;

    public static NonDominatedSortingFactory getOriginalImplementationV1() {
        return INSTANCE_O1;
    }

    public static NonDominatedSortingFactory getReorderingImplementationV1() {
        return INSTANCE_R1;
    }

    public static NonDominatedSortingFactory getReorderingImplementationV2() {
        return INSTANCE_R2;
    }

    public static NonDominatedSortingFactory getRandomizedQuadraticImplementationV1() {
        return INSTANCE_RQ1;
    }

    public static NonDominatedSortingFactory getRandomizedQuadraticImplementationV2() {
        return INSTANCE_RQ2;
    }

    public static NonDominatedSortingFactory getDeterministicQuadraticImplementationV1() {
        return INSTANCE_DQ1;
    }

    public static NonDominatedSortingFactory getDeterministicQuadraticImplementationV2() {
        return INSTANCE_DQ2;
    }

    public static NonDominatedSortingFactory getDeterministicQuadraticImplementationV3() {
        return INSTANCE_DQ3;
    }

    public static NonDominatedSortingFactory getDeterministicQuadraticImplementationV4() {
        return INSTANCE_DQ4;
    }
}
