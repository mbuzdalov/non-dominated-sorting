package ru.ifmo.nds.ndt;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.ArraySorter;

import java.util.Arrays;

public class ENS_NDT_OneTree extends NonDominatedSorting {
    private SplitBuilder splitBuilder;
    private TreeRankNode tree;
    private int[] ranks;
    private double[][] transposedPoints;
    private double[][] points;
    private final int threshold;

    public ENS_NDT_OneTree(int maximumPoints, int maximumDimension, int threshold) {
        super(maximumPoints, maximumDimension);
        this.threshold = threshold;
        tree = TreeRankNode.EMPTY;
        ranks = new int[maximumPoints];
        transposedPoints = new double[maximumDimension][];
        for (int d = 1; d < maximumDimension; ++d) {
            transposedPoints[d] = new double[maximumPoints];
        }
        splitBuilder = new SplitBuilder(transposedPoints, maximumPoints);
        points = new double[maximumPoints][];
    }

    @Override
    public String getName() {
        return "ENS-NDT OneTree (Objects, threshold = " + threshold + ")";
    }

    @Override
    protected void closeImpl() {
        splitBuilder = null;
        tree = null;
        ranks = null;
        transposedPoints = null;
        points = null;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = points.length;
        int dim = points[0].length;
        ArrayHelper.fillIdentity(indices, n);
        sorter.lexicographicalSort(points, indices, 0, n, points[0].length);

        int newN = ArraySorter.retainUniquePoints(points, indices, this.points, ranks);
        Arrays.fill(this.ranks, 0, newN, 0);

        tree = TreeRankNode.EMPTY;
        for (int i = 0; i < newN; ++i) {
            for (int j = 1; j < dim; ++j) {
                transposedPoints[j][i] = this.points[i][j];
            }
        }

        Split split = splitBuilder.result(newN, dim, threshold);

        tree = tree.add(this.points[0], 0, split, threshold);
        for (int i = 1; i < newN; ++i) {
            double[] current = this.points[i];
            this.ranks[i] = tree.evaluateRank(current, 0, split, dim);
            if (this.ranks[i] <= maximalMeaningfulRank) {
                tree = tree.add(current, this.ranks[i], split, threshold);
            }
        }

        for (int i = 0; i < n; ++i) {
            ranks[i] = this.ranks[ranks[i]];
            this.points[i] = null;
        }
    }
}