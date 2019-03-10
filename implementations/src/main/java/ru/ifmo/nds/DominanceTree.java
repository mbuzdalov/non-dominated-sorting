package ru.ifmo.nds;

import ru.ifmo.nds.domtree.*;

public final class DominanceTree {
    public enum InsertionOption {
        NO_DELAYED_INSERTION(),
        DELAYED_INSERTION_SEQUENTIAL_CONCATENATION(),
        DELAYED_INSERTION_RECURSIVE_CONCATENATION()
    }

    private DominanceTree() {}

    public static NonDominatedSortingFactory getNoPresortInsertion(boolean useRecursiveMerge) {
        return (maximumPoints, maximumDimension) -> new NoPresort(maximumPoints, maximumDimension, useRecursiveMerge);
    }

    public static NonDominatedSortingFactory getPresortInsertion(boolean useRecursiveMerge, InsertionOption insertionOption) {
        switch (insertionOption) {
            case NO_DELAYED_INSERTION:
                return (maximumPoints, maximumDimension) -> new PresortNoDelayed(maximumPoints, maximumDimension, useRecursiveMerge);
            case DELAYED_INSERTION_SEQUENTIAL_CONCATENATION:
                return (maximumPoints, maximumDimension) -> new PresortDelayedNoRecursion(maximumPoints, maximumDimension, useRecursiveMerge);
            case DELAYED_INSERTION_RECURSIVE_CONCATENATION:
                return (maximumPoints, maximumDimension) -> new PresortDelayedRecursion(maximumPoints, maximumDimension, useRecursiveMerge);
            default:
                throw new IllegalArgumentException("Illegal insertion option: " + insertionOption);
        }
    }
}
