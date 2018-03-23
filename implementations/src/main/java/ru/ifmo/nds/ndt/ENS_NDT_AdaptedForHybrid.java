package ru.ifmo.nds.ndt;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.DoubleArraySorter;

import java.util.Arrays;

public class ENS_NDT_AdaptedForHybrid extends NonDominatedSorting {
    private DoubleArraySorter sorter;
    private SplitBuilderOneTree splitBuilder;
    private TreeRankNode tree;
    private int[] indices;
    private int[] ranks;
    private double[][] transposedPoints;
    private double[][] points;
    private final int threshold;

    private int[] rankReindex;
    private int[] compressedRanks;

    public ENS_NDT_AdaptedForHybrid(int maximumPoints, int maximumDimension, int threshold) {
        super(maximumPoints, maximumDimension);
        this.threshold = threshold;
        this.sorter = new DoubleArraySorter(maximumPoints);
        this.splitBuilder = new SplitBuilderOneTree(maximumPoints);
        this.tree = TreeRankNode.EMPTY;
        this.indices = new int[maximumPoints];
        this.ranks = new int[maximumPoints];
        this.transposedPoints = new double[maximumDimension][maximumPoints];
        this.points = new double[maximumPoints][];

        this.rankReindex = new int[maximumPoints];
        this.compressedRanks = new int[maximumPoints];
    }

    @Override
    public String getName() {
        return "ENS-NDT (Objects, threshold = " + threshold + ")";
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

        rankReindex = null;
        compressedRanks = null;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        throw new UnsupportedOperationException("ENS_NDT_AdaptedForHybrid sorting doesn't work alone");
    }

    public void sortHelperB(double[][] points,
                            int[] ranks,
                            int goodFrom,
                            int goodUntil,
                            int weakFrom,
                            int weakUntil,
                            int M,
                            int maximalMeaningfulRank) {
        System.arraycopy(ranks, goodFrom, this.ranks, goodFrom, goodUntil - goodFrom);
        System.arraycopy(ranks, weakFrom, this.ranks, weakFrom, weakUntil - weakFrom);
//
        for (int i = weakFrom; i < weakUntil; ++i) {
            indices[i] = i;
            points[i][M] = ranks[i]; // для сжатия
        }
        for (int i = goodFrom; i < goodUntil; ++i) {
            indices[i] = i;
        }

        // Для сжатия точек из weak добавим еще один критерий - текущий ранг.
        // Всегда M < maximumDimension в HelperB
        int newWeakUntil = weakFrom + DoubleArraySorter.retainUniquePoints(points, indices, this.points, rankReindex, weakFrom, weakFrom, weakUntil, M + 1);

        int newGoodUntil = goodFrom + DoubleArraySorter.retainUniquePoints(points, indices, this.points, rankReindex, goodFrom, goodFrom, goodUntil, M);

        initializeCompressedRanks(goodFrom, goodUntil, weakFrom, weakUntil); // TODO можно убрать

        initializeTransposedPoints(goodFrom, newGoodUntil, weakFrom, newWeakUntil, M);

        Split split = splitBuilder.result(transposedPoints, goodFrom, newGoodUntil, M, threshold);

        tree = tree.add(this.points[goodFrom], this.compressedRanks[goodFrom], split, threshold);
        for (int i = goodFrom + 1; i < newGoodUntil; ++i) {
            tree = tree.add(this.points[i], this.compressedRanks[i], split, threshold);
        }
        // TODO изменить порядок обхода

        for (int i = weakFrom; i < newWeakUntil; ++i) {
            this.compressedRanks[i] = tree.evaluateRank(this.points[i], this.compressedRanks[i], split);
        }

        Arrays.fill(this.points, weakFrom, weakFrom, null);
        Arrays.fill(this.points, goodFrom, goodFrom, null);
        for (int i = weakFrom; i < weakUntil; ++i) {
            ranks[i] = this.compressedRanks[rankReindex[i]];
        }
    }

    private void initializeTransposedPoints(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int dim) {
        for (int i = goodFrom; i < goodUntil; ++i) {
            for (int j = 0; j < dim; ++j) {
                transposedPoints[j][i] = this.points[i][j];
            }
        }
        for (int i = weakFrom; i < weakUntil; ++i) {
            for (int j = 0; j < dim; ++j) {
                transposedPoints[j][i] = this.points[i][j];
            }
        }
    }

    private void initializeCompressedRanks(int goodFrom, int goodUntil, int weakFrom, int weakUntil) {
        Arrays.fill(compressedRanks, goodFrom, goodUntil, -1);
        for (int i = goodFrom; i < goodUntil; ++i) {
            compressedRanks[rankReindex[i]] = Math.max(compressedRanks[rankReindex[i]], this.ranks[i]);
        }
        for (int i = weakFrom; i < weakUntil; ++i) {
            compressedRanks[rankReindex[i]] = this.ranks[i];
        }
    }

}
