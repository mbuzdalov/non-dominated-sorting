package ru.ifmo.nds.ndt;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.DoubleArraySorter;

import java.util.Arrays;

public class ENS_NDT_AdaptedForHybrid extends NonDominatedSorting {
    private SplitBuilder splitBuilder;
    private DoubleArraySorter sorter;
    private TreeRankNode tree;
    private double[][] transposedPoints;
    private final int threshold;
    private int[] indices;
    private int[] resolver;

    public ENS_NDT_AdaptedForHybrid(int maximumPoints, int maximumDimension, int threshold) {
        super(maximumPoints, maximumDimension);
        this.threshold = threshold;
        this.splitBuilder = new SplitBuilder(maximumPoints);
        this.tree = TreeRankNode.EMPTY;
        this.transposedPoints = new double[maximumDimension][maximumPoints];
        this.indices = new int[maximumPoints];
        this.resolver = new int[maximumPoints];
        this.sorter = new DoubleArraySorter(maximumPoints);
    }

    @Override
    public String getName() {
        return "ENS-NDT (Objects, threshold = " + threshold + ")";
    }

    @Override
    protected void closeImpl() {
        splitBuilder = null;
        tree = null;
        transposedPoints = null;
        indices = null;
        resolver = null;
        sorter = null;
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

        for (int i = goodFrom; i < goodUntil; ++i) {
            for (int j = 0; j < M; ++j) {
                transposedPoints[j][i] = points[i][j];
            }
        }
        for (int i = weakFrom; i < weakUntil; ++i) {
            for (int j = 0; j < M; ++j) {
                transposedPoints[j][i] = points[i][j];
            }
        }

        for (int i = goodFrom; i < goodUntil; ++i) {
            indices[i - goodFrom] = i;
        }
        for (int i = weakFrom; i < weakUntil; ++i) {
            indices[goodUntil - goodFrom + i - weakFrom] = i;
        }
        int sizeUnion = goodUntil - goodFrom + weakUntil - weakFrom;
        for (int i = goodFrom; i < goodUntil; ++i) {
            resolver[i] = i;
        }
        for (int i = weakFrom; i < weakUntil; ++i) {
            resolver[i] = i;
        }
        sorter.sortWhileResolvingEqual(points, indices, 0, sizeUnion, 0, resolver);

        Split split = splitBuilder.result(transposedPoints, goodFrom, goodUntil, M, threshold);

        tree = TreeRankNode.EMPTY;

        //  лексикографический обход
        for (int i = 0; i < sizeUnion; ++i) {
            int id = indices[i];
            if (id >= goodFrom && id < goodUntil) {
                tree = tree.add(points[id], ranks[id], split, threshold);
                continue;
            }

            ranks[id] = tree.evaluateRank(points[id], ranks[id], split, M);
        }


        tree = null;
    }

    public void sortHelperB(double[][] points,
                            int[] ranks,
                            int from,
                            int until,
                            int M,
                            int maximalMeaningfulRank) {

        for (int i = from; i < until; ++i) {
            for (int j = 0; j < M; ++j) {
                transposedPoints[j][i] = points[i][j];
            }
        }

        Split split = splitBuilder.result(transposedPoints, from, until, M, threshold);

        tree = TreeRankNode.EMPTY;
        tree = tree.add(points[from], ranks[from], split, threshold);
        for (int i = from + 1; i < until; ++i) {
            ranks[i] = tree.evaluateRank(points[i], ranks[i], split, M);

            if (ranks[i] <= maximalMeaningfulRank) {
                tree = tree.add(points[i], ranks[i], split, threshold);
            }
        }

        tree = null;
    }

}
