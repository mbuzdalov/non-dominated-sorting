package ru.ifmo.nds;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.*;

public class PGFPlotBuilder {
    private static final String INPUT_OPTION = "--input";
    private static final String OUTPUT_OPTION = "--output";

    private static void printUsageAndExit(String errorMessage) {
        if (errorMessage != null) {
            System.err.println("Error: " + errorMessage);
        }
        System.err.println("Usage: ru.ifmo.nds.PGFPlotBuilder [options] where options are:");
        System.err.println("    --input <filename> <name>");
        System.err.println("        Include JMH log from file <filename> under name <name> in the plots.");
        System.err.println("    --output <filename>");
        System.out.println("        Print LaTeX output to <filename> instead of standard output.");
        System.exit(1);
        throw new AssertionError("System.exit is banned from stopping the program");
    }

    private static class SinglePlot {
        private final PlotDescriptor myDescriptor;
        private final Map<String, List<JMHBenchmarkResult>> myResults = new LinkedHashMap<>();

        private SinglePlot(PlotDescriptor myDescriptor) {
            this.myDescriptor = myDescriptor;
        }

        void addResult(String plotName, JMHBenchmarkResult result) {
            myResults.computeIfAbsent(plotName, k -> new ArrayList<>()).add(result);
        }

        void print(PrintWriter out) {
            out.println("% " + myDescriptor.toString());
            out.println("\\begin{tikzpicture}");
            out.println("\\begin{axis}[xtick=data, xmode=log, ymode=log,");
            out.println("              width=\\textwidth, height=0.45\\textheight, legend pos=north west,");
            out.println("              xlabel={" + myDescriptor.toString() + "}]");
            for (Map.Entry<String, List<JMHBenchmarkResult>> plot : myResults.entrySet()) {
                out.println("\\addplot plot[error bars/.cd, y dir=both, y explicit] table[y error plus=y-max, y error minus=y-min] {");
                out.println("    x y y-min y-max");
                for (JMHBenchmarkResult plotPoint : plot.getValue()) {
                    out.print("    " + plotPoint.getParameters().get("N"));
                    double min = Double.POSITIVE_INFINITY, sum = 0, max = Double.NEGATIVE_INFINITY;
                    List<Double> points = plotPoint.getResults();
                    for (double r : points) {
                        min = Math.min(min, r);
                        max = Math.max(max, r);
                        sum += r;
                    }
                    double avg = sum / points.size();
                    out.println(" " + avg + " " + (avg - min) + " " + (max - avg));
                }
                out.println("};");
                out.println("\\addlegendentry{" + plot.getKey() + "};");
            }
            out.println("\\end{axis}");
            out.println("\\end{tikzpicture}\\vspace{2ex}");
            out.println();
        }

        static class PlotDescriptor implements Comparable<PlotDescriptor> {
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
        String outputFile = null;

        for (int i = 0; i < args.length; ++i) {
            if (args[i].equals(INPUT_OPTION)) {
                if (i + 2 >= args.length) {
                    printUsageAndExit("Last " + INPUT_OPTION + " followed by too few arguments");
                }
                try {
                    String algorithmName = args[i + 2];
                    for (JMHBenchmarkResult result : JMHLogParser.parse(args[i + 1])) {
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
            } else if (args[i].equals(OUTPUT_OPTION)) {
                if (i + 1 >= args.length) {
                    printUsageAndExit("Last " + OUTPUT_OPTION + " followed by no arguments");
                }
                outputFile = args[i + 1];
                i += 1;
            }
        }

        try (PrintWriter out = outputFile == null ? new PrintWriter(System.out) : new PrintWriter(outputFile)) {
            out.println("\\documentclass{extreport}");
            out.println("\\usepackage{geometry}");
            out.println("\\geometry{a4paper,top=2cm,bottom=2cm,left=2cm,right=2cm}");
            out.println("\\usepackage[T2A]{fontenc}");
            out.println("\\usepackage[utf8]{inputenc}");
            out.println("\\usepackage{pgfplots}");
            out.println("\\pgfplotsset{compat=newest}");
            out.println("\\begin{document}");
            for (SinglePlot plot : plots.values()) {
                plot.print(out);
            }
            out.println("\\end{document}");
        } catch (FileNotFoundException ex) {
            StringWriter out = new StringWriter();
            PrintWriter pw = new PrintWriter(out);
            ex.printStackTrace(pw);
            pw.println();
            pw.println("Error: file '" + outputFile + "' cannot be written to");
            pw.close();
            printUsageAndExit(out.toString());
        }
    }
}
