package ru.ifmo.nds.plotting;

import ru.ifmo.nds.PlotBuilder;
import ru.ifmo.nds.rundb.IdUtils;
import ru.ifmo.nds.rundb.Record;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class Plotly {
    private Plotly() {}

    private static String divIdFor(PlotBuilder.SinglePlot plot) {
        return "hash" + System.identityHashCode(plot.myDatasetId);
    }

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

    private static void printPlotly(PlotBuilder.SinglePlot singlePlot, String factor, PrintWriter out) {
        out.println("Plotly.newPlot('" + divIdFor(singlePlot) + "', [");
        for (Map.Entry<String, List<Record>> plot : singlePlot.myResults.entrySet()) {
            out.println("  {");
            out.println("    name: '" + plot.getKey() + "',");
            out.println("    type: 'scatter',");
            List<Long> x = new ArrayList<>();
            List<Double> y = new ArrayList<>();
            List<Double> yPlus = new ArrayList<>();
            List<Double> yMinus = new ArrayList<>();

            Map<Long, Stats> stats = new TreeMap<>();
            for (Record plotPoint : plot.getValue()) {
                long N = IdUtils.extract(plotPoint.getDatasetId(), factor);
                Stats st = stats.computeIfAbsent(N, v -> new Stats());
                for (double p : plotPoint.getReleaseMeasurements()) {
                    st.add(p);
                }
            }

            for (Map.Entry<Long, Stats> s : stats.entrySet()) {
                double avg = s.getValue().sum / s.getValue().count;
                double errMin = avg - s.getValue().min;
                double errMax = s.getValue().max - avg;
                x.add(s.getKey());
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
        out.println("  title: 'Dataset: " + singlePlot.myDatasetId + "',");
        out.println("  xaxis: { type: 'log', autorange: true, title: 'Number of points' },");
        out.println("  yaxis: { type: 'log', autorange: true, title: 'Runtime, seconds' },");
        out.println("});");
    }

    public static void printPlotly(Map<String, PlotBuilder.SinglePlot> plots, String factor, Path output) {
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
                printPlotly(plot, factor, out);
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
