package ru.ifmo.nds.ndt;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DoubleArraySorter;

import java.util.Arrays;

public class ENS_NDT_OneTree extends NonDominatedSorting {
    private DoubleArraySorter sorter;
    private SplitBuilder splitBuilder;
    private TreeRankNode tree;
    private int[] indices;
    private int[] ranks;
    private double[][] transposedPoints;
    private double[][] points;
    private final int threshold;

    public ENS_NDT_OneTree(int maximumPoints, int maximumDimension, int threshold) {
        super(maximumPoints, maximumDimension);
        this.threshold = threshold;
        this.sorter = new DoubleArraySorter(maximumPoints);
        this.splitBuilder = new SplitBuilder(maximumPoints);
        this.tree = TreeRankNode.EMPTY;
        this.indices = new int[maximumPoints];
        this.ranks = new int[maximumPoints];
        this.transposedPoints = new double[maximumDimension][maximumPoints];
        this.points = new double[maximumPoints][];
    }

    @Override
    public String getName() {
        return "ENS-NDT OneTree (Objects, threshold = " + threshold + ")";
    }

    @Override
    protected void closeImpl() {
        sorter = null;
        splitBuilder = null;
        tree = null;
        indices = null;
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

        if (dim == 1) {
            int currRank = ranks[indices[0]] = 0;
            double prevValue = points[indices[0]][0];
            for (int i = 1; i < n; ++i) {
                int ii = indices[i];
                double currValue = points[ii][0];
                if (prevValue != currValue) {
                    ++currRank;
                    prevValue = currValue;
                }
                ranks[ii] = currRank;
            }
            return;
        }

        int newN = DoubleArraySorter.retainUniquePoints(points, indices, this.points, ranks);
        Arrays.fill(this.ranks, 0, newN, 0);

        tree = TreeRankNode.EMPTY;
        for (int i = 0; i < newN; ++i) {
            for (int j = 0; j < dim; ++j) {
                transposedPoints[j][i] = this.points[i][j];
            }
        }

        Split split = splitBuilder.result(transposedPoints, newN, dim, threshold);

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
