package ru.ifmo.jmh;

import ru.ifmo.JensenFortinBuzdalov;
import ru.ifmo.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_JensenFortinBuzdalov_RedBlackTree extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return JensenFortinBuzdalov.getRedBlackTreeSweepImplementation();
    }
}
