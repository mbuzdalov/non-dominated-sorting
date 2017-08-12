package ru.ifmo.nds;

import ru.ifmo.nds.domtree.NoPresort;
import ru.ifmo.nds.domtree.Presort;

public class DominanceTree {
    public enum InsertionOption {
        NO_DELAYED_INSERTION("no delayed insertion"),
        DELAYED_INSERTION_SEQUENTIAL_CONCATENATION("delayed insertion with sequential concatenation"),
        DELAYED_INSERTION_RECURSIVE_CONCATENATION("delayed insertion with recursive concatenation");

        private final String humanReadableDescription;

        InsertionOption(String humanReadableDescription) {
            this.humanReadableDescription = humanReadableDescription;
        }

        public String humanReadableDescription() {
            return humanReadableDescription;
        }
    }

    private DominanceTree() {}

    public static NonDominatedSortingFactory getNoPresortInsertion(boolean useRecursiveMerge) {
        return (maximumPoints, maximumDimension) -> new NoPresort(maximumPoints, maximumDimension, useRecursiveMerge);
    }

    public static NonDominatedSortingFactory getPresortInsertion(boolean useRecursiveMerge, InsertionOption insertionOption) {
        return (maximumPoints, maximumDimension) -> new Presort(maximumPoints, maximumDimension, useRecursiveMerge, insertionOption);
    }
}
