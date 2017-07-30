package ru.ifmo.nds;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class PGFPlotBuilder {
    private static final String INPUT_OPTION = "--input";
    private static final String INPUT_IGNORE_OPTION = "--input-ignore";
    private static final String OUTPUT_OPTION = "--output";
    private static final String INPUT_FILE_LIST_OPTION = "--input-file-list";

    private static void printUsageAndExit(String errorMessage) {
        if (errorMessage != null) {
            System.err.println("Error: " + errorMessage);
        }
        System.err.println("Usage: ru.ifmo.nds.PGFPlotBuilder [options] where options are:");
        System.err.println("    --input <filename> <name>");
        System.err.println("        Include JMH log from file <filename> under name <name> in the plots.");
        System.err.println("    --input-file-list <filename>");
        System.err.println("        Read file <filename> and load JMH logs from this file.");
        System.err.println("        Every line starting with '+' is interpreted.");
        System.err.println("        The first non-whitespace token immediately after it should be a filename.");
        System.err.println("        Everything after the whitespace is interpreted as the plot's name.");
        System.err.println("    --output <filename>");
        System.err.println("        Print LaTeX output to <filename> instead of standard output.");
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
            out.println("\\section*{" + myDescriptor.toString() + "}");
            out.println("\\begin{tikzpicture}");
            out.println("\\begin{axis}[xtick=data, xmode=log, ymode=log,");
            out.println("              width=\\textwidth, height=0.45\\textheight, legend pos=north west,");
            out.println("              ymin=1e-7, ymax=4, cycle list name=my custom]");

            StringWriter tableBuilder = new StringWriter();
            PrintWriter tableWriter = new PrintWriter(tableBuilder);

            for (Map.Entry<String, List<JMHBenchmarkResult>> plot : myResults.entrySet()) {
                out.println("\\addplot plot[error bars/.cd, y dir=both, y explicit] table[y error plus=y-max, y error minus=y-min] {");
                out.println("    x y y-min y-max");
                for (JMHBenchmarkResult plotPoint : plot.getValue()) {
                    int N = plotPoint.getParameters().get("N");
                    out.print("    " + N);
                    List<Double> points = plotPoint.getResults();
                    double min = Double.POSITIVE_INFINITY, sum = 0, max = Double.NEGATIVE_INFINITY;
                    for (double r : points) {
                        min = Math.min(min, r);
                        max = Math.max(max, r);
                        sum += r;
                    }
                    double avg = sum / points.size();
                    double errMin = avg - min;
                    double errMax = max - avg;
                    tableWriter.println("    {" + plot.getKey() + "} " + N + " " + min + " " + avg + " " + max);
                    out.println(" " + avg + " " + errMin + " " + errMax);
                }
                out.println("};");
                out.println("\\addlegendentry{" + plot.getKey() + "};");
            }
            out.println("\\end{axis}");
            out.println("\\end{tikzpicture}\\vspace{2ex}");
            out.println();
            out.println("\\pgfplotstabletypeset[sci zerofill, columns/Algo/.style={string type}] {");
            out.println("    Algo N Tmin Tavg Tmax");
            out.println(tableBuilder.getBuffer());
            out.println("}");
            out.println("\\newpage");
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
                case OUTPUT_OPTION:
                    if (i + 1 >= args.length) {
                        printUsageAndExit("Last " + OUTPUT_OPTION + " followed by no arguments");
                    }
                    outputFile = args[i + 1];
                    i += 1;
                    break;
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
            out.println("\\usepackage{pgfplotstable}");
            out.println("\\pgfplotscreateplotcyclelist{my custom}{%");
            out.println("blue!60!black,every mark/.append style={fill=blue!60!black},mark=*\\\\%1");
            out.println("red!70!black,every mark/.append style={fill=red!70!black},mark=*\\\\%2");
            out.println("green!70!black,every mark/.append style={fill=green!70!black},mark=*\\\\%3");
            out.println("gray,every mark/.append style={fill=gray},mark=*\\\\%4");
            out.println("orange,every mark/.append style={fill=orange},mark=*\\\\%5");
            out.println("brown!80!black,every mark/.append style={fill=brown!80!black},mark=*\\\\%6");
            out.println("green,every mark/.append style={fill=green},mark=*\\\\%7");
            out.println("violet!80!black,every mark/.append style={fill=violet!80!black},mark=*\\\\%8");
            out.println("black,every mark/.append style={fill=black},mark=*\\\\%9");
            out.println("teal,every mark/.append style={fill=teal},mark=*\\\\%10");
            out.println("magenta!70!black,every mark/.append style={fill=magenta!70!black},mark=*\\\\%11");
            out.println("yellow!90!black,every mark/.append style={fill=yellow!90!black},mark=*\\\\%12");
            out.println("blue!60!black,every mark/.append style={fill=blue!60!black},mark=star\\\\%13");
            out.println("red!70!black,every mark/.append style={fill=red!70!black},mark=star\\\\%14");
            out.println("green!70!black,every mark/.append style={fill=green!70!black},mark=star\\\\%15");
            out.println("gray,every mark/.append style={fill=gray},mark=star\\\\%16");
            out.println("orange,every mark/.append style={fill=orange},mark=star\\\\%17");
            out.println("brown!80!black,every mark/.append style={fill=brown!80!black},mark=star\\\\%18");
            out.println("green,every mark/.append style={fill=green},mark=star\\\\%19");
            out.println("violet!80!black,every mark/.append style={fill=violet!80!black},mark=star\\\\%20");
            out.println("black,every mark/.append style={fill=black},mark=star\\\\%21");
            out.println("teal,every mark/.append style={fill=teal},mark=star\\\\%22");
            out.println("magenta!70!black,every mark/.append style={fill=magenta!70!black},mark=star\\\\%23");
            out.println("yellow!90!black,every mark/.append style={fill=yellow!90!black},mark=star\\\\%24");
            out.println("}");
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
