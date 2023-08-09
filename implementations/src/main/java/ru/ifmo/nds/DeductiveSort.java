package ru.ifmo.nds;

import ru.ifmo.nds.deductive.*;

public final class DeductiveSort {
    private DeductiveSort() {}

    private static final NonDominatedSortingFactory INSTANCE_O1 = OriginalV1::new;
    private static final NonDominatedSortingFactory INSTANCE_O2 = OriginalV2::new;
    private static final NonDominatedSortingFactory INSTANCE_O3 = (maximumPoints, maximumDimension) ->
            new OriginalV3(maximumPoints, maximumDimension, false);
    private static final NonDominatedSortingFactory INSTANCE_O3_RANDOMIZED = (maximumPoints, maximumDimension) ->
            new OriginalV3(maximumPoints, maximumDimension, true);
    private static final NonDominatedSortingFactory INSTANCE_R1 = ReorderingV1::new;
    private static final NonDominatedSortingFactory INSTANCE_R2 = ReorderingV2::new;
    private static final NonDominatedSortingFactory INSTANCE_DQ1 = DeterministicQuadraticV1::new;
    private static final NonDominatedSortingFactory INSTANCE_DQ2 = DeterministicQuadraticV2::new;
    private static final NonDominatedSortingFactory INSTANCE_DQ3 = DeterministicQuadraticV3::new;
    private static final NonDominatedSortingFactory INSTANCE_DQ4 = DeterministicQuadraticV4::new;


    public static NonDominatedSortingFactory getOriginalImplementationV1() {
        return INSTANCE_O1;
    }

    public static NonDominatedSortingFactory getOriginalImplementationV2() {
        return INSTANCE_O2;
    }

    public static NonDominatedSortingFactory getOriginalImplementationV3() {
        return INSTANCE_O3;
    }

    public static NonDominatedSortingFactory getReorderingImplementationV1() {
        return INSTANCE_R1;
    }

    public static NonDominatedSortingFactory getReorderingImplementationV2() {
        return INSTANCE_R2;
    }

    public static NonDominatedSortingFactory getRandomizedOriginalImplementationV3() {
        return INSTANCE_O3_RANDOMIZED;
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
