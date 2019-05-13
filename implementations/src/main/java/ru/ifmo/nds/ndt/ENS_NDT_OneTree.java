package ru.ifmo.nds.ndt;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.ArraySorter;

public class ENS_NDT_OneTree extends NonDominatedSorting {
    private SplitBuilder splitBuilder;
    private int[] ranks;
    private double[][] transposedPoints;
    private double[][] points;
    private final int threshold;

    public ENS_NDT_OneTree(int maximumPoints, int maximumDimension, int threshold) {
        super(maximumPoints, maximumDimension);
        this.threshold = threshold;
        ranks = new int[maximumPoints];
        transposedPoints = new double[maximumDimension][];
        for (int d = 1; d < maximumDimension; ++d) {
            transposedPoints[d] = new double[maximumPoints];
        }
        splitBuilder = new SplitBuilder(transposedPoints, maximumPoints, threshold);
        points = new double[maximumPoints][];
    }

    @Override
    public String getName() {
        return "ENS-NDT OneTree (Objects, threshold = " + threshold + ")";
    }

    @Override
    protected void closeImpl() {
        splitBuilder = null;
        ranks = null;
        transposedPoints = null;
        points = null;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = points.length;
        int dim = points[0].length;
        ArrayHelper.fillIdentity(indices, n);
        sorter.lexicographicalSort(points, indices, 0, n, dim);

        int newN = ArraySorter.retainUniquePoints(points, indices, this.points, ranks);
        this.ranks[0] = 0;

        TreeRankNode tree = threshold == 1 ? TreeRankNode.EMPTY_1 : TreeRankNode.EMPTY;
        for (int i = 0; i < newN; ++i) {
            for (int j = 1; j < dim; ++j) {
                transposedPoints[j][i] = this.points[i][j];
            }
        }

        Split split = splitBuilder.result(newN, dim);
        TreeRankNode.RankEvaluationContext ctx = new TreeRankNode.RankEvaluationContext();
        ctx.maxObj = dim - 1;

        tree = tree.add(this.points[0], 0, split, threshold);
        for (int i = 1; i < newN; ++i) {
            double[] current = this.points[i];
            ctx.rank = 0;
            ctx.point = current;
            tree.evaluateRank(ctx, split);
            int currRank = ctx.rank;
            this.ranks[i] = currRank;
            if (currRank <= maximalMeaningfulRank) {
                tree = tree.add(current, currRank, split, threshold);
            }
        }

        for (int i = 0; i < n; ++i) {
            ranks[i] = this.ranks[ranks[i]];
            this.points[i] = null;
        }
    }
}
