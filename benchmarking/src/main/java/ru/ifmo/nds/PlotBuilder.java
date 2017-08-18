package ru.ifmo.nds;

import ru.ifmo.nds.plotting.LaTeX;
import ru.ifmo.nds.plotting.Plotly;
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

    private static final Comparator<String> ID_LEX_COMPARATOR = (o1, o2) -> {
        StringTokenizer s1 = new StringTokenizer(o1, ".");
        StringTokenizer s2 = new StringTokenizer(o2, ".");
        while (true) {
            if (s1.hasMoreTokens() && s2.hasMoreTokens()) {
                String t1 = s1.nextToken();
                String t2 = s2.nextToken();
                int l1 = t1.length() - 1, l2 = t2.length() - 1;
                while (l1 >= 0 && Character.isDigit(t1.charAt(l1))) --l1;
                while (l2 >= 0 && Character.isDigit(t2.charAt(l2))) --l2;
                ++l1;
                ++l2;
                if (l1 != t1.length() && l2 != t2.length()) {
                    String prefix1 = t1.substring(0, l1);
                    String prefix2 = t2.substring(0, l2);
                    int prefixCmp = prefix1.compareTo(prefix2);
                    if (prefixCmp != 0) {
                        return prefixCmp;
                    }
                    long suffix1 = Long.parseLong(t1.substring(l1));
                    long suffix2 = Long.parseLong(t2.substring(l2));
                    if (suffix1 != suffix2) {
                        return Long.compare(suffix1, suffix2);
                    }
                } else {
                    int cmp = t1.compareTo(t2);
                    if (cmp != 0) {
                        return cmp;
                    }
                }
            } else if (s1.hasMoreTokens()) {
                return -1;
            } else if (s2.hasMoreTokens()) {
                return 1;
            } else {
                return 0;
            }
        }
    };

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

    private static String factorize(String s, String factor) {
        StringBuilder rv = new StringBuilder();
        boolean first = true;
        StringTokenizer st = new StringTokenizer(s, ".");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (!token.startsWith(factor) || (token.length() != factor.length() && !Character.isDigit(token.charAt(factor.length())))) {
                if (first) {
                    first = false;
                } else {
                    rv.append('.');
                }
                rv.append(token);
            }
        }
        return rv.toString();
    }

    public static long extract(String s, String factor) {
        StringTokenizer st = new StringTokenizer(s, ".");
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.startsWith(factor)) {
                try {
                    return Long.parseLong(token.substring(factor.length()));
                } catch (NumberFormatException ex) {
                    // continue
                }
            }
        }
        throw new IllegalArgumentException("Cannot extract '" + factor + "' from string '" + s + "'");
    }

    public static void main(String[] args) {
        Map<String, SinglePlot> plots = new TreeMap<>(ID_LEX_COMPARATOR);
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
                    String factorizedDataset = factorize(result.getDatasetId(), factor);
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
