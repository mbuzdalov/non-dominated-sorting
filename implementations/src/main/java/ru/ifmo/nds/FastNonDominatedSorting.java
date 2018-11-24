package ru.ifmo.nds;

import ru.ifmo.nds.fnds.LinearMemory;
import ru.ifmo.nds.fnds.OriginalVersion;

public final class FastNonDominatedSorting {
    private FastNonDominatedSorting() {}

    private static final NonDominatedSortingFactory ORIGINAL_FACTORY = OriginalVersion::new;
    private static final NonDominatedSortingFactory LINEAR_MEMORY_FACTORY = LinearMemory::new;

    public static NonDominatedSortingFactory getOriginalVersion() {
        return ORIGINAL_FACTORY;
    }

    public static NonDominatedSortingFactory getLinearMemoryImplementation() {
        return LINEAR_MEMORY_FACTORY;
    }
}
