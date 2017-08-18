package ru.ifmo.nds.jmh;

import ru.ifmo.nds.JensenFortinBuzdalov;
import ru.ifmo.nds.NonDominatedSortingFactory;

@SuppressWarnings("unused")
public class JMH_JensenFortinBuzdalov_RedBlackTree extends AbstractBenchmark {
    @Override
    public NonDominatedSortingFactory getFactory() {
        return JensenFortinBuzdalov.getRedBlackTreeSweepImplementation();
    }
}
