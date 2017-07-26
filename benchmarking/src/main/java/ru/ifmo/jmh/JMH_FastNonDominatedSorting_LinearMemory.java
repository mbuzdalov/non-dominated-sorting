package ru.ifmo.jmh;

import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import ru.ifmo.FastNonDominatedSorting;
import ru.ifmo.NonDominatedSortingFactory;

public class JMH_FastNonDominatedSorting_LinearMemory extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return FastNonDominatedSorting.getLinearMemoryImplementation();
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(JMH_FastNonDominatedSorting_LinearMemory.class.getSimpleName()).build();
        new Runner(opt).run();
    }
}
