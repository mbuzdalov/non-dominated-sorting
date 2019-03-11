package ru.ifmo.nds.ndt;

import java.util.Arrays;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.ArraySorter;

public class ENS_NDT extends NonDominatedSorting {
    private SplitBuilder splitBuilder;
    private TreeNode[] levels;
    private int[] ranks;
    private double[][] transposedPoints;
    private double[][] points;
    private final int threshold;

    public ENS_NDT(int maximumPoints, int maximumDimension, int threshold) {
        super(maximumPoints, maximumDimension);
        this.threshold = threshold;
        this.splitBuilder = new SplitBuilder(maximumPoints);
        this.levels = new TreeNode[maximumPoints];
        this.ranks = new int[maximumPoints];
        this.transposedPoints = new double[maximumDimension][];
        for (int d = 1; d < maximumDimension; ++d) {
            this.transposedPoints[d] = new double[maximumPoints];
        }
        this.points = new double[maximumPoints][];
    }

    @Override
    public String getName() {
        return "ENS-NDT (Objects, threshold = " + threshold + ")";
    }

    @Override
    protected void closeImpl() {
        splitBuilder = null;
        levels = null;
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

        for (int i = 0; i < newN; ++i) {
            levels[i] = TreeNode.EMPTY;
            for (int j = 1; j < dim; ++j) {
                transposedPoints[j][i] = this.points[i][j];
            }
        }

        Split split = splitBuilder.result(transposedPoints, newN, dim, threshold);

        int maxRank = 1;
        levels[0] = levels[0].add(this.points[0], split, threshold);
        for (int i = 1; i < newN; ++i) {
            double[] current = this.points[i];
            if (levels[0].dominates(current, split)) {
                int left = 0, right = maxRank;
                while (right - left > 1) {
                    int mid = (left + right) >>> 1;
                    if (levels[mid].dominates(current, split)) {
                        left = mid;
                    } else {
                        right = mid;
                    }
                }
                int rank = left + 1;
                this.ranks[i] = rank;
                if (rank <= maximalMeaningfulRank) {
                    levels[rank] = levels[rank].add(current, split, threshold);
                    if (rank == maxRank) {
                        ++maxRank;
                    }
                }
            } else {
                levels[0] = levels[0].add(current, split, threshold);
            }
        }

        for (int i = 0; i < n; ++i) {
            ranks[i] = this.ranks[ranks[i]];
            this.points[i] = null;
        }
    }
}
