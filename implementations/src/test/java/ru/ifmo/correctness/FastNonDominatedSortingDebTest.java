package ru.ifmo.correctness;

import ru.ifmo.FastNonDominatedSortingDeb;
import ru.ifmo.NonDominatedSortingFactory;

import java.util.Arrays;
import java.util.List;

public class FastNonDominatedSortingDebTest extends CorrectnessTestsBase {
    @Override
    protected List<NonDominatedSortingFactory> getFactories() {
        return Arrays.asList(
                FastNonDominatedSortingDeb.getOriginalImplementation(),
                FastNonDominatedSortingDeb.getLinearMemoryImplementation()
        );
    }
}
