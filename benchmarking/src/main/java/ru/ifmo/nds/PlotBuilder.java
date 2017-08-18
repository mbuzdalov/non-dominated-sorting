package ru.ifmo.nds;

import ru.ifmo.nds.plotting.LaTeX;
import ru.ifmo.nds.plotting.Plotly;
import ru.ifmo.nds.rundb.IdUtils;
import ru.ifmo.nds.rundb.Record;
import ru.ifmo.nds.rundb.Records;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;

public class PlotBuilder {
    private static final String INPUT_OPTION = "--input";
    private static final String FACTOR_OPTION = "--factor";
    private static final String OUTPUT_LATEX_OPTION = "--output-latex";
    private static final String OUTPUT_PLOTLY_OPTION = "--output-plotly";

    public static void printUsageAndExit(String errorMessage) {
        if (errorMessage != null) {
            System.err.println("Error: " + errorMessage);
        }
        System.err.println("Usage: ru.ifmo.nds.PlotBuilder [options] where options are:");
        System.err.println("    --input <filename>");
        System.err.println("        Include a JSON database with records from the file <filename>.");
        System.err.println("    --factor <parameter>");
        System.err.println("        Tell the system to build plots factoring on the given parameter.");
        System.err.println("    --output-latex <filename>");
        System.err.println("        Print LaTeX output to <filename>.");
        System.err.println("    --output-plotly <filename>");
        System.err.println("        Print an HTML with Plot.ly charts to <filename>.");
        System.exit(1);
        throw new AssertionError("System.exit is banned from stopping the program");
    }

    public static class SinglePlot {
        public final String myDatasetId;
        public final Map<String, List<Record>> myResults = new TreeMap<>();

        private SinglePlot(String myDatasetId) {
            this.myDatasetId = myDatasetId;
        }

        private void addResult(String plotName, Record result) {
            myResults.computeIfAbsent(plotName, k -> new ArrayList<>()).add(result);
        }
    }

    public static void main(String[] args) {
        Map<String, SinglePlot> plots = new TreeMap<>(IdUtils.getLexicographicalIdComparator());
        String factor = null;
        List<BiConsumer<Map<String, SinglePlot>, String>> printCommands = new ArrayList<>();
        List<String> files = new ArrayList<>();

        for (int i = 0; i < args.length; ++i) {
            switch (args[i]) {
                case INPUT_OPTION:
                    if (i + 1 >= args.length) {
                        printUsageAndExit("Last " + INPUT_OPTION + " followed by too few arguments");
                    }
                    files.add(args[i + 1]);
                    i += 1;
                    break;
                case FACTOR_OPTION:
                    if (i + 1 >= args.length) {
                        printUsageAndExit("Last " + FACTOR_OPTION + " followed by too few arguments");
                    }
                    factor = args[i + 1];
                    i += 1;
                    break;
                case OUTPUT_LATEX_OPTION:
                    if (i + 1 >= args.length) {
                        printUsageAndExit("Last " + OUTPUT_LATEX_OPTION + " followed by no arguments");
                    } else {
                        Path target = Paths.get(args[i + 1]);
                        printCommands.add((plot, f) -> LaTeX.printLaTeX(plot, f, target));
                        i += 1;
                    }
                    break;
                case OUTPUT_PLOTLY_OPTION:
                    if (i + 1 >= args.length) {
                        printUsageAndExit("Last " + OUTPUT_PLOTLY_OPTION + " followed by no arguments");
                    } else {
                        Path target = Paths.get(args[i + 1]);
                        printCommands.add((plot, f) -> Plotly.printPlotly(plot, f, target));
                        i += 1;
                    }
                    break;
                default:
                    printUsageAndExit("Unknown argument: '" + args[i] + "'");
                    break;
            }
        }

        if (factor == null) {
            printUsageAndExit("There should be exactly one --factor command");
        }

        for (String file : files) {
            try {
                for (Record result : Records.loadFromFile(Paths.get(file))) {
                    String factorizedDataset = IdUtils.factorize(result.getDatasetId(), factor);
                    plots.computeIfAbsent(factorizedDataset, SinglePlot::new).addResult(result.getAlgorithmId(), result);
                }
            } catch (Exception ex) {
                StringWriter out = new StringWriter();
                PrintWriter pw = new PrintWriter(out);
                ex.printStackTrace(pw);
                pw.println();
                pw.println("Error: file '" + file + "' cannot be parsed");
                pw.close();
                printUsageAndExit(out.toString());
            }
        }

        for (BiConsumer<Map<String, SinglePlot>, String> action : printCommands) {
            action.accept(plots, factor);
        }
    }
}
