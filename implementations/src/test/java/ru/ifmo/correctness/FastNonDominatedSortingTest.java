package ru.ifmo.correctness;

import ru.ifmo.FastNonDominatedSorting;
import ru.ifmo.NonDominatedSortingFactory;

import java.util.Arrays;
import java.util.List;

public class FastNonDominatedSortingTest extends CorrectnessTestsBase {
    @Override
    protected List<NonDominatedSortingFactory> getFactories() {
        return Arrays.asList(
                FastNonDominatedSorting.getOriginalImplementation(),
                FastNonDominatedSorting.getLinearMemoryImplementation()
        );
    }
}
