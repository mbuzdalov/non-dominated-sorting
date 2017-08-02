package ru.ifmo.jmh;

import ru.ifmo.JensenFortinBuzdalov;
import ru.ifmo.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_JensenFortinBuzdalov_RedBlackTreeHybridLinearNDS extends AbstractBenchmark {
    @Override
    protected NonDominatedSortingFactory getFactory() {
        return JensenFortinBuzdalov.getRedBlackTreeSweepHybridImplementation();
    }
}
