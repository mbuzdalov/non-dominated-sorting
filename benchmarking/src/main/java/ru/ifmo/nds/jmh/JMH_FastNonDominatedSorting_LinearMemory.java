package ru.ifmo.nds.jmh;

import ru.ifmo.nds.FastNonDominatedSorting;
import ru.ifmo.nds.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_FastNonDominatedSorting_LinearMemory extends AbstractBenchmark {
    @Override
    public NonDominatedSortingFactory getFactory() {
        return FastNonDominatedSorting.getLinearMemoryImplementation();
    }
}
