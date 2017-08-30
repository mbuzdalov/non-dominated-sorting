package ru.ifmo.nds.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.validators.PositiveInteger;

import ru.ifmo.nds.IdCollection;
import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.NonDominatedSortingFactory;
import ru.ifmo.nds.rundb.Dataset;

public class RunOnTest extends JCommanderRunnable {
    @Parameter(names = "--algorithmId",
            required = true,
            description = "Specify the algorithm ID to benchmark.")
    private String algorithmId;

    @Parameter(names = "--datasetId",
            required = true,
            description = "Specify the dataset ID to benchmark.")
    private String datasetId;

    @Parameter(names = "--times",
            required = true,
            description = "Specify how many times to run.")
    private Integer times;

    @Parameter(names = "--max-rank",
            description = "Specify maximum meaningful rank.",
            validateWith = PositiveInteger.class)
    private Integer maxRank;

    @Override
    protected void run() throws CLIWrapperException {
        Dataset dataset;
        try {
            dataset = IdCollection.getDataset(datasetId);
        } catch (IllegalArgumentException ex) {
            throw new CLIWrapperException("Cannot find dataset '" + datasetId + "'.", ex);
        }
        NonDominatedSortingFactory factory;
        try {
            factory = IdCollection.getNonDominatedSortingFactory(algorithmId);
        } catch (IllegalArgumentException ex) {
            throw new CLIWrapperException("Cannot find algorithm '" + algorithmId + "'.", ex);
        }

        NonDominatedSorting algorithm = factory.getInstance(dataset.getMaxNumberOfPoints(), dataset.getMaxDimension());
        int realMaxRank = maxRank == null ? dataset.getMaxNumberOfPoints() : maxRank;

        for (int i = 0, limit = times; i < limit; ++i) {
            long t0 = System.nanoTime();
            int sumMaximumRanks = dataset.runAlgorithm(algorithm, realMaxRank);
            long timeNS = System.nanoTime() - t0;
            System.out.printf("Run #%d: time %.03g seconds, sum of ranks = %d%n", i + 1, timeNS * 1e-9, sumMaximumRanks);
        }
    }

    public static void main(String[] args) {
        JCommanderRunnable.run(new RunOnTest(), args);
    }
}
