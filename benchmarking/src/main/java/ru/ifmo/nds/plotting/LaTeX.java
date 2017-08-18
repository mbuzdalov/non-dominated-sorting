package ru.ifmo.nds.plotting;

import ru.ifmo.nds.PlotBuilder;
import ru.ifmo.nds.rundb.Record;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class LaTeX {
    private LaTeX() {}

    private static class Stats {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double sum = 0;
        int count = 0;

        void add(double value) {
            this.min = Math.min(this.min, value);
            this.max = Math.max(this.max, value);
            sum += value;
            ++count;
        }
    }

    private static void printLaTeX(PlotBuilder.SinglePlot singlePlot, String factor, PrintWriter out) {
        out.println("% " + singlePlot.myDatasetId);
        out.println("\\section*{Dataset: " + singlePlot.myDatasetId + "}");
        out.println("\\begin{tikzpicture}[scale=0.65]");
        out.println("\\begin{axis}[xtick=data, xmode=log, ymode=log,");
        out.println("              width=\\textwidth, height=0.7\\textheight, legend pos=outer north east,");
        out.println("              ymin=3e-7, ymax=6, cycle list name=my custom]");

        StringWriter tableBuilder = new StringWriter();
        PrintWriter tableWriter = new PrintWriter(tableBuilder);

        Set<Long> factors = new TreeSet<>();
        for (Map.Entry<String, List<Record>> plot : singlePlot.myResults.entrySet()) {
            out.println("\\addplot plot[error bars/.cd, y dir=both, y explicit] table[y error plus=y-max, y error minus=y-min] {");
            out.println("    x y y-min y-max");
            tableWriter.print("    {" + plot.getKey() + "}");
            TreeMap<Long, Stats> stats = new TreeMap<>();
            for (Record plotPoint : plot.getValue()) {
                long N = PlotBuilder.extract(plotPoint.getDatasetId(), factor);
                List<Double> points = plotPoint.getReleaseMeasurements();
                Stats st = stats.computeIfAbsent(N, v -> new Stats());
                for (double p : points) {
                    st.add(p);
                }
            }
            for (Map.Entry<Long, Stats> entry : stats.entrySet()) {
                double avg = entry.getValue().sum / entry.getValue().count;
                double errMin = avg - entry.getValue().min;
                double errMax = entry.getValue().max - avg;
                tableWriter.print(" " + avg);
                out.println("    " + entry.getKey() + " " + avg + " " + errMin + " " + errMax);
            }
            out.println("};");
            out.println("\\addlegendentry{" + plot.getKey() + "};");
            tableWriter.println();
            factors.addAll(stats.keySet());
        }
        out.println("\\end{axis}");
        out.println("\\end{tikzpicture}\\vspace{2ex}");
        out.println();
        out.println("\\pgfplotstabletypeset[font=\\footnotesize, sci zerofill, columns/Algo/.style={string type}] {");
        out.print("    Algo");
        for (long f : factors) {
            out.print(" T" + f);
        }
        out.println();
        out.println(tableBuilder.getBuffer());
        out.println("}");
        out.println("\\newpage");
    }

    public static void printLaTeX(Map<String, PlotBuilder.SinglePlot> plots, String factor, Path output) {
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(output))) {
            out.println("\\documentclass{extreport}");
            out.println("\\usepackage{geometry}");
            out.println("\\geometry{a4paper,landscape,top=2cm,bottom=2cm,left=2cm,right=2cm}");
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
            for (PlotBuilder.SinglePlot plot : plots.values()) {
                printLaTeX(plot, factor, out);
            }
            out.println("\\end{document}");
        } catch (IOException ex) {
            StringWriter out = new StringWriter();
            PrintWriter pw = new PrintWriter(out);
            ex.printStackTrace(pw);
            pw.println();
            pw.println("Error: file '" + output.toString() + "' cannot be written to");
            pw.close();
            PlotBuilder.printUsageAndExit(out.toString());
        }
    }
}
