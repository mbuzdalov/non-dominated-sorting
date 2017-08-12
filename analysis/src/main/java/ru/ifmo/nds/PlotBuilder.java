package ru.ifmo.nds;

import ru.ifmo.nds.plotting.LaTeX;
import ru.ifmo.nds.plotting.Plotly;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Consumer;

public class PlotBuilder {
    private static final String INPUT_OPTION = "--input";
    private static final String INPUT_IGNORE_OPTION = "--input-ignore";
    private static final String OUTPUT_LATEX_OPTION = "--output-latex";
    private static final String OUTPUT_PLOTLY_OPTION = "--output-plotly";
    private static final String INPUT_FILE_LIST_OPTION = "--input-file-list";

    public static void printUsageAndExit(String errorMessage) {
        if (errorMessage != null) {
            System.err.println("Error: " + errorMessage);
        }
        System.err.println("Usage: ru.ifmo.nds.PlotBuilder [options] where options are:");
        System.err.println("    --input <filename> <name>");
        System.err.println("        Include JMH log from file <filename> under name <name> in the plots.");
        System.err.println("    --input-file-list <filename>");
        System.err.println("        Read file <filename> and load JMH logs from this file.");
        System.err.println("        Every line starting with '+' is interpreted.");
        System.err.println("        The first non-whitespace token immediately after it should be a filename.");
        System.err.println("        Everything after the whitespace is interpreted as the plot's name.");
        System.err.println("    --output-latex <filename>");
        System.err.println("        Print LaTeX output to <filename>.");
        System.err.println("    --output-plotly <filename>");
        System.err.println("        Print an HTML with Plot.ly charts to <filename>.");
        System.exit(1);
        throw new AssertionError("System.exit is banned from stopping the program");
    }

    public static class SinglePlot {
        public final PlotDescriptor myDescriptor;
        public final Map<String, List<JMHBenchmarkResult>> myResults = new LinkedHashMap<>();

        private SinglePlot(PlotDescriptor myDescriptor) {
            this.myDescriptor = myDescriptor;
        }

        private void addResult(String plotName, JMHBenchmarkResult result) {
            myResults.computeIfAbsent(plotName, k -> new ArrayList<>()).add(result);
        }

        public static class PlotDescriptor implements Comparable<PlotDescriptor> {
            private final TreeMap<String, Integer> parameters;
            private final String benchmarkName;

            PlotDescriptor(JMHBenchmarkResult result) {
                this.benchmarkName = result.getBenchmarkName();
                this.parameters = new TreeMap<>(result.getParameters());
                this.parameters.remove("N");
            }

            @Override
            public String toString() {
                StringBuilder sb = new StringBuilder();
                sb.append(benchmarkName);
                if (parameters.size() > 0) {
                    sb.append(" [");
                    boolean first = true;
                    for (Map.Entry<String, Integer> param : parameters.entrySet()) {
                        if (!first) {
                            sb.append(", ");
                        }
                        first = false;
                        sb.append(param.getKey()).append("=").append(param.getValue());
                    }
                    sb.append("]");
                }
                return sb.toString();
            }

            @Override
            public int compareTo(PlotDescriptor o) {
                int bn = benchmarkName.compareTo(o.benchmarkName);
                if (bn != 0) return bn;
                Iterator<Map.Entry<String, Integer>>
                        thisItr = parameters.entrySet().iterator(),
                        thatItr = o.parameters.entrySet().iterator();
                while (true) {
                    if (thisItr.hasNext()) {
                        if (!thatItr.hasNext()) {
                            return 1;
                        } else {
                            Map.Entry<String, Integer>
                                    thisEntry = thisItr.next(),
                                    thatEntry = thatItr.next();
                            int kc = thisEntry.getKey().compareTo(thatEntry.getKey());
                            if (kc != 0) return kc;
                            int vc = thisEntry.getValue().compareTo(thatEntry.getValue());
                            if (vc != 0) return vc;
                        }
                    } else {
                        return thatItr.hasNext() ? -1 : 0;
                    }
                }
            }

            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                PlotDescriptor that = (PlotDescriptor) o;
                return parameters.equals(that.parameters) && benchmarkName.equals(that.benchmarkName);
            }

            @Override
            public int hashCode() {
                int result = parameters.hashCode();
                result = 31 * result + benchmarkName.hashCode();
                return result;
            }
        }
    }

    public static void main(String[] args) {
        Map<SinglePlot.PlotDescriptor, SinglePlot> plots = new TreeMap<>();
        List<Consumer<Map<SinglePlot.PlotDescriptor, SinglePlot>>> printCommands = new ArrayList<>();

        for (int i = 0; i < args.length; ++i) {
            switch (args[i]) {
                case INPUT_OPTION:
                    if (i + 2 >= args.length) {
                        printUsageAndExit("Last " + INPUT_OPTION + " followed by too few arguments");
                    }
                    try {
                        String algorithmName = args[i + 2];
                        for (JMHBenchmarkResult result : JMHLogParser.parse(Paths.get(args[i + 1]))) {
                            SinglePlot.PlotDescriptor desc = new SinglePlot.PlotDescriptor(result);
                            plots.computeIfAbsent(desc, SinglePlot::new).addResult(algorithmName, result);
                        }
                        i += 2;
                    } catch (Exception ex) {
                        StringWriter out = new StringWriter();
                        PrintWriter pw = new PrintWriter(out);
                        ex.printStackTrace(pw);
                        pw.println();
                        pw.println("Error: file '" + args[i + 1] + "' cannot be parsed");
                        pw.close();
                        printUsageAndExit(out.toString());
                    }
                    break;
                case INPUT_IGNORE_OPTION:
                    if (i + 2 >= args.length) {
                        printUsageAndExit("Last " + INPUT_IGNORE_OPTION + " followed by too few arguments");
                    }
                    break;
                case INPUT_FILE_LIST_OPTION:
                    if (i + 1 >= args.length) {
                        printUsageAndExit("Last " + INPUT_FILE_LIST_OPTION + " followed by too few arguments");
                    }
                    try {
                        Path file = Paths.get(args[i + 1]);
                        int[] lineNumber = {0};
                        Files.lines(file).forEachOrdered(s -> {
                            ++lineNumber[0];
                            if (s.startsWith("+")) {
                                s = s.substring(1);
                                int firstWS = s.indexOf(' ');
                                String fileName = firstWS == -1 ? s : s.substring(0, firstWS);
                                String plotName = firstWS == -1 ? s : s.substring(firstWS + 1).trim();
                                Path plotFile = file.resolveSibling(fileName);
                                try {
                                    for (JMHBenchmarkResult result : JMHLogParser.parse(plotFile)) {
                                        SinglePlot.PlotDescriptor desc = new SinglePlot.PlotDescriptor(result);
                                        plots.computeIfAbsent(desc, SinglePlot::new).addResult(plotName, result);
                                    }
                                } catch (IOException e) {
                                    throw new UncheckedIOException(e);
                                }
                            }
                        });
                    } catch (Exception ex) {
                        StringWriter out = new StringWriter();
                        PrintWriter pw = new PrintWriter(out);
                        ex.printStackTrace(pw);
                        pw.println();
                        pw.println("Error: file '" + args[i + 1] + "' cannot be parsed");
                        pw.close();
                        printUsageAndExit(out.toString());
                    }
                    break;
                case OUTPUT_LATEX_OPTION:
                    if (i + 1 >= args.length) {
                        printUsageAndExit("Last " + OUTPUT_LATEX_OPTION + " followed by no arguments");
                    } else {
                        Path target = Paths.get(args[i + 1]);
                        printCommands.add(plot -> LaTeX.printLaTeX(plot, target));
                        i += 1;
                    }
                    break;
                case OUTPUT_PLOTLY_OPTION:
                    if (i + 1 >= args.length) {
                        printUsageAndExit("Last " + OUTPUT_PLOTLY_OPTION + " followed by no arguments");
                    } else {
                        Path target = Paths.get(args[i + 1]);
                        printCommands.add(plot -> Plotly.printPlotly(plot, target));
                        i += 1;
                    }
                    break;
            }
        }
        printCommands.forEach(action -> action.accept(plots));
    }
}
