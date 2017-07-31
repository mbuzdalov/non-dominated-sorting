package ru.ifmo;

import ru.ifmo.domtree.NoPresort;

public class DominanceTree {
    private DominanceTree() {}

    public static NonDominatedSortingFactory getNoDelayedInsertion(boolean useRecursiveMerge) {
        return (maximumPoints, maximumDimension) -> new NoPresort(maximumPoints, maximumDimension, useRecursiveMerge);
    }
}
