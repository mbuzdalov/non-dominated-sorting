package ru.ifmo.nds;

import ru.ifmo.nds.mnds.BitSetImplementation;

public final class SetIntersectionSort {
    private static final NonDominatedSortingFactory BIT_SET_INSTANCE = BitSetImplementation::new;

    public static NonDominatedSortingFactory getBitSetInstance() {
        return BIT_SET_INSTANCE;
    }
}
