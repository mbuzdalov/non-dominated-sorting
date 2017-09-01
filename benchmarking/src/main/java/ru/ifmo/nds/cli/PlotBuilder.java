package ru.ifmo.nds.cli;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.beust.jcommander.Parameter;

import ru.ifmo.nds.plotting.LaTeX;
import ru.ifmo.nds.plotting.Plotly;
import ru.ifmo.nds.rundb.IdUtils;
import ru.ifmo.nds.rundb.Record;
import ru.ifmo.nds.rundb.Records;

public class PlotBuilder extends JCommanderRunnable {
    @Parameter(names = "--input",
            variableArity = true,
            required = true,
            description = "Specify input files to build plots from.")
    private List<String> inputFiles;

    @Parameter(names = "--factor",
            required = true,
            description = "Specify the factor to be put onto the X axis.")
    private String factor;

    @Parameter(names = "--output-latex",
            variableArity = true,
            description = "Print LaTeX output to the given file.")
    private List<String> outputLatex;

    @Parameter(names = "--output-plotly",
            variableArity = true,
            description = "Print Plot.ly HTML output to the given file.")
    private List<String> outputPlotly;

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

    @Override
    protected void run() throws CLIWrapperException {
        Map<String, SinglePlot> plots = new TreeMap<>(IdUtils.getLexicographicalIdComparator());

        for (String file : inputFiles) {
            try {
                for (Record result : Records.loadFromFile(Paths.get(file))) {
                    String factorizedDataset = IdUtils.factorize(result.getDatasetId(), factor);
                    plots.computeIfAbsent(factorizedDataset, SinglePlot::new).addResult(result.getAlgorithmId(), result);
                }
            } catch (Exception ex) {
                throw new CLIWrapperException("Cannot read file '" + file + "'.", ex);
            }
        }

        if (outputLatex != null) {
            for (String file : outputLatex) {
                try {
                    LaTeX.printLaTeX(plots, factor, Paths.get(file));
                } catch (IOException ex) {
                    throw new CLIWrapperException("Cannot write LaTeX file '" + file + "'.", ex);
                }
            }
        }
        if (outputPlotly != null) {
            for (String file : outputPlotly) {
                try {
                    Plotly.printPlotly(plots, factor, Paths.get(file));
                } catch (IOException ex) {
                    throw new CLIWrapperException("Cannot write Plot.ly file '" + file + "'.", ex);
                }
            }
        }
    }

    public static void main(String[] args) {
        JCommanderRunnable.run(new PlotBuilder(), args);
    }
}
