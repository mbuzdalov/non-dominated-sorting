package ru.ifmo.nds.jmh.main;

import java.util.Set;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import ru.ifmo.nds.IdCollection;
import ru.ifmo.nds.jmh.UniformCorrelated;
import ru.ifmo.nds.jmh.UniformHypercube;
import ru.ifmo.nds.jmh.UniformHyperplanes;

public class Minimal {
    public static void main(String[] args) throws RunnerException {
        String outputFile = args.length > 0 ? args[0] : "jmh-output.json";
        Set<String> allSortings = IdCollection.getAllNonDominatedSortingIDs();

        Options options = new OptionsBuilder()
                .include(UniformHypercube.class.getName())
                .include(UniformHyperplanes.class.getName())
                .include(UniformCorrelated.class.getName())
                .param("algorithmId", allSortings.toArray(new String[0]))
                .resultFormat(ResultFormatType.JSON)
                .result(outputFile)
                .build();
        new Runner(options).run();
    }
}
