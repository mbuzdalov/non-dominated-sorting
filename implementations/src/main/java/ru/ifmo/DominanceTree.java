package ru.ifmo;

import ru.ifmo.domtree.NoDelayedInsertion;

public class DominanceTree {
    private DominanceTree() {}

    private static final NonDominatedSortingFactory NO_DELAYED_INSERTION = NoDelayedInsertion::new;

    public static NonDominatedSortingFactory getNoDelayedInsertion() {
        return NO_DELAYED_INSERTION;
    }
}
