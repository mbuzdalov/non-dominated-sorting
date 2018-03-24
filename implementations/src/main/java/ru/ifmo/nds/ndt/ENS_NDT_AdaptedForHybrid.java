package ru.ifmo.nds.ndt;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.DoubleArraySorter;

import java.util.Arrays;

public class ENS_NDT_AdaptedForHybrid extends NonDominatedSorting {
    private SplitBuilder splitBuilder;
    private TreeRankNode tree;
    private double[][] transposedPoints;
    private final int threshold;

    public ENS_NDT_AdaptedForHybrid(int maximumPoints, int maximumDimension, int threshold) {
        super(maximumPoints, maximumDimension);
        this.threshold = threshold;
        this.splitBuilder = new SplitBuilder(maximumPoints);
        this.tree = TreeRankNode.EMPTY;
        this.transposedPoints = new double[maximumDimension][maximumPoints];
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

        Split split = splitBuilder.result(transposedPoints, goodFrom, goodUntil, M, threshold);
        tree = TreeRankNode.EMPTY;
        tree = tree.add(points[goodFrom], ranks[goodFrom], split, threshold);
        for (int i = goodFrom + 1; i < goodUntil; ++i) {
            tree = tree.add(points[i], ranks[i], split, threshold);
        }
        // TODO подумать про порядок обхода
        // правильным будет лексикографический обход?

        for (int i = weakFrom; i < weakUntil; ++i) {
            int resultRank = tree.evaluateRank(points[i], ranks[i], split, M); // TODO delete
            if(resultRank != ranks[i]) {
                ranks[i] = resultRank;
            }
        }

        tree = null;
        Arrays.fill(points, weakFrom, weakFrom, null);
        Arrays.fill(points, goodFrom, goodFrom, null);

    }

}
