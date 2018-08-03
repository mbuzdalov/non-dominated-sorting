package ru.ifmo.nds.jmh.main;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import ru.ifmo.nds.IdCollection;

public class GlobalSmall {
    public static void main(String[] args) throws RunnerException {
        Options options = new OptionsBuilder()
                .include(UniformHypercube.class.getSimpleName())
                .include(UniformHyperplanes.class.getSimpleName())
                .param("algorithmId", IdCollection.getAllNonDominatedSortingIDs().toArray(new String[0]))
                .resultFormat(ResultFormatType.JSON)
                .build();
        new Runner(options).run();
    }
}
