package ru.ifmo;

import ru.ifmo.fnds.LinearMemory;
import ru.ifmo.fnds.OriginalVersion;

/**
 *
 */
public class FastNonDominatedSorting {
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
