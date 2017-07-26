package ru.ifmo.jmh;

import ru.ifmo.FastNonDominatedSorting;
import ru.ifmo.NonDominatedSortingFactory;

@SuppressWarnings({"unused"})
public class JMH_FastNonDominatedSorting_OriginalVersion extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return FastNonDominatedSorting.getOriginalImplementation();
    }
}
