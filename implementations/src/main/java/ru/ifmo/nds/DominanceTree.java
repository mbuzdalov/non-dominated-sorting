package ru.ifmo.nds;

import ru.ifmo.nds.domtree.*;

public final class DominanceTree {
    private DominanceTree() {}

    public static NonDominatedSortingFactory getNoPresortInsertion(boolean useRecursiveMerge) {
        return (maximumPoints, maximumDimension) -> new NoPresort(maximumPoints, maximumDimension, useRecursiveMerge);
    }

    public static NonDominatedSortingFactory getPresortInsertion(boolean useRecursiveMerge, boolean useDelayedInsertion) {
        if (useDelayedInsertion) {
            return (maximumPoints, maximumDimension) -> new PresortNoDelayed(maximumPoints, maximumDimension, useRecursiveMerge);
        } else {
            return (maximumPoints, maximumDimension) -> new PresortDelayed(maximumPoints, maximumDimension, useRecursiveMerge);
        }
    }
}
