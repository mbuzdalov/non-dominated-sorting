package ru.ifmo.nds.plotting;

import ru.ifmo.nds.JMHBenchmarkResult;
import ru.ifmo.nds.PlotBuilder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Plotly {
    private Plotly() {}

    private static String divIdFor(PlotBuilder.SinglePlot plot) {
        return "hash" + System.identityHashCode(plot.myDescriptor);
    }

    private static void printPlotly(PlotBuilder.SinglePlot singlePlot, PrintWriter out) {
        out.println("Plotly.newPlot('" + divIdFor(singlePlot) + "', [");
        for (Map.Entry<String, List<JMHBenchmarkResult>> plot : singlePlot.myResults.entrySet()) {
            out.println("  {");
            out.println("    name: '" + plot.getKey() + "',");
            out.println("    type: 'scatter',");
            List<Integer> x = new ArrayList<>();
            List<Double> y = new ArrayList<>();
            List<Double> yPlus = new ArrayList<>();
            List<Double> yMinus = new ArrayList<>();
            for (JMHBenchmarkResult plotPoint : plot.getValue()) {
                x.add(plotPoint.getParameters().get("N"));
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
                y.add(avg);
                yPlus.add(errMax);
                yMinus.add(errMin);
            }
            out.println("    x: " + x + ",");
            out.println("    y: " + y + ",");
            out.println("    error_y: {");
            out.println("      type: 'data', symmetric: false,");
            out.println("      array: " + yPlus + ",");
            out.println("      arrayminus: " + yMinus);
            out.println("    }");
            out.println("  },");
        }
        out.println("], {");
        out.println("  title: 'Dataset: " + singlePlot.myDescriptor.toString() + "',");
        out.println("  xaxis: { type: 'log', autorange: true, title: 'Number of points' },");
        out.println("  yaxis: { type: 'log', autorange: true, title: 'Runtime, seconds' },");
        out.println("});");
    }

    public static void printPlotly(Map<PlotBuilder.SinglePlot.PlotDescriptor, PlotBuilder.SinglePlot> plots, Path output) {
        try (PrintWriter out = new PrintWriter(Files.newBufferedWriter(output))) {
            out.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
            out.println("<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">");
            out.println("<html xmlns=\"http://www.w3.org/1999/xhtml\">");
            out.println("<head>");
            out.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\"/>");
            out.println("<title>Non-dominated sorting plots</title>");
            out.println("<script src=\"https://cdn.plot.ly/plotly-latest.min.js\"></script>");
            out.println("</head>");
            out.println("<body>");
            for (PlotBuilder.SinglePlot plot : plots.values()) {
                out.println("<div id=\"" + divIdFor(plot) + "\" style=\"min-height: 768px\"></div>");
            }
            out.println("<script>");
            for (PlotBuilder.SinglePlot plot : plots.values()) {
                printPlotly(plot, out);
            }
            out.println("</script>");
            out.println("</body>");
            out.println("</html>");
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
