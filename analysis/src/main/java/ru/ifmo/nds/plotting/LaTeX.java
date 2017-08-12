package ru.ifmo.nds.plotting;

import ru.ifmo.nds.JMHBenchmarkResult;
import ru.ifmo.nds.PlotBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class LaTeX {
    private LaTeX() {}

    private static void printLaTeX(PlotBuilder.SinglePlot singlePlot, PrintWriter out) {
        out.println("% " + singlePlot.myDescriptor.toString());
        out.println("\\section*{Dataset: " + singlePlot.myDescriptor.toString() + "}");
        out.println("\\begin{tikzpicture}[scale=0.65]");
        out.println("\\begin{axis}[xtick=data, xmode=log, ymode=log,");
        out.println("              width=\\textwidth, height=0.7\\textheight, legend pos=outer north east,");
        out.println("              ymin=1e-11, ymax=3e-7, cycle list name=my custom]");

        StringWriter tableBuilder = new StringWriter();
        PrintWriter tableWriter = new PrintWriter(tableBuilder);

        for (Map.Entry<String, List<JMHBenchmarkResult>> plot : singlePlot.myResults.entrySet()) {
            out.println("\\addplot plot[error bars/.cd, y dir=both, y explicit] table[y error plus=y-max, y error minus=y-min] {");
            out.println("    x y y-min y-max");
            tableWriter.print("    {" + plot.getKey() + "}");
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
                tableWriter.print(" " + avg);
                out.println(" " + (avg / N / N) + " " + (errMin / N / N) + " " + (errMax / N / N));
            }
            out.println("};");
            out.println("\\addlegendentry{" + plot.getKey() + "};");
            tableWriter.println();
        }
        out.println("\\end{axis}");
        out.println("\\end{tikzpicture}\\vspace{2ex}");
        out.println();
        out.println("\\pgfplotstabletypeset[font=\\footnotesize, sci zerofill, columns/Algo/.style={string type}] {");
        out.println("    Algo T10 T100 T1000 T10000");
        out.println(tableBuilder.getBuffer());
        out.println("}");
        out.println("\\newpage");
    }

    public static void printLaTeX(Map<PlotBuilder.SinglePlot.PlotDescriptor, PlotBuilder.SinglePlot> plots, Path output) {
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
                printLaTeX(plot, out);
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
