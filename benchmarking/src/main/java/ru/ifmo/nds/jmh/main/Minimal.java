package ru.ifmo.nds.jmh.main;

import java.io.File;
import java.io.IOException;
import java.util.*;

import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import ru.ifmo.nds.IdCollection;
import ru.ifmo.nds.jmh.AntiOriginalDeductiveSort;
import ru.ifmo.nds.jmh.UniformCorrelated;
import ru.ifmo.nds.jmh.UniformHypercube;
import ru.ifmo.nds.jmh.UniformHyperplanes;

public class Minimal {
    private static final List<String> minimalN = Arrays.asList("10", "100", "1000");
    private static final List<String> minimalD = Arrays.asList("2", "3", "5", "10");
    private static final List<String> minimalF = Arrays.asList("1", "2", "n/2", "n");

    private static final List<String> moreMinimalN = Arrays.asList("3", "31", "316", "3162", "10000");
    private static final List<String> moreMinimalD = Arrays.asList("4", "6", "7", "8", "9", "11", "12", "13", "14", "15");

    public static void main(String[] args) throws RunnerException, IOException {
        Set<String> allAlgorithms = IdCollection.getAllNonDominatedSortingIDs();
        Set<String> algorithms = new TreeSet<>(allAlgorithms);

        final String[] stub = new String[0];

        List<String> n = new ArrayList<>();
        List<String> d = new ArrayList<>();
        List<String> f = new ArrayList<>(minimalF);

        String outputFile = "jmh-output.json";

        boolean failed = false;
        boolean useGiven = false;

        for (String s : args) {
            if (s.startsWith("--out=")) {
                outputFile = s.substring("--out=".length());
            } else if (s.startsWith("--use=")) {
                if (useGiven) {
                    failed = true;
                    System.err.println("Error: multiple --use directives given");
                } else {
                    useGiven = true;
                    String cmd = s.substring("--use=".length());
                    switch (cmd) {
                        case "min":
                            n.addAll(minimalN);
                            d.addAll(minimalD);
                            break;
                        case "more-d":
                            n.addAll(minimalN);
                            d.addAll(moreMinimalD);
                            break;
                        case "more-n":
                            n.addAll(moreMinimalN);
                            d.addAll(minimalD);
                            d.addAll(moreMinimalD);
                            d.sort(Comparator.comparingInt(Integer::parseInt));
                            break;
                        default:
                            System.err.println("Error: unknown option to --use: '" + cmd + "'");
                    }
                }
            } else if (s.startsWith("--algo=")) {
                StringTokenizer st = new StringTokenizer(s.substring("--algo=".length()), ",");
                algorithms.clear();
                while (st.hasMoreTokens()) {
                    String token = st.nextToken();
                    if (!allAlgorithms.contains(token)) {
                        failed = true;
                        System.err.println("Error: unknown algorithm '" + token + "' passed to --algo");
                        break;
                    } else {
                        algorithms.add(token);
                    }
                }
            } else if (s.startsWith("--n=")) {
                StringTokenizer st = new StringTokenizer(s.substring("--n=".length()), ",");
                n.clear();
                while (st.hasMoreTokens()) {
                    n.add(st.nextToken());
                }
            } else if (s.startsWith("--d=")) {
                StringTokenizer st = new StringTokenizer(s.substring("--d=".length()), ",");
                d.clear();
                while (st.hasMoreTokens()) {
                    d.add(st.nextToken());
                }
            } else {
                failed = true;
                System.err.println("Error: unknown command '" + s + "'");
            }
        }

        failed |= n.isEmpty();
        failed |= d.isEmpty();

        if (failed) {
            System.err.println("Usage: Minimal --use=<min|more-d|more-n> [--algo=<algo1>,<algo2>,...] [--out=<file>]");
            System.exit(1);
        }

        String tmpFile = outputFile + ".tmp";

        Options options = new OptionsBuilder()
                .include(UniformHypercube.class.getName())
                .include(UniformHyperplanes.class.getName())
                .include(UniformCorrelated.class.getName())
                .include(AntiOriginalDeductiveSort.class.getName())
                .param("algorithmId", algorithms.toArray(stub))
                .param("n", n.toArray(stub))
                .param("d", d.toArray(stub))
                .param("f", f.toArray(stub))
                .resultFormat(ResultFormatType.JSON)
                .result(tmpFile)
                .build();
        new Runner(options).run();

        File tmp = new File(tmpFile);
        PatchWithOSHI.patch(tmp, new File(outputFile));
        if (!tmp.delete()) {
            System.err.println("Could not delete the temporary file: \"" + tmpFile + "\"");
        }
    }
}
