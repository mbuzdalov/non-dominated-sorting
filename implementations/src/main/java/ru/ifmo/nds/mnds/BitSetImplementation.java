package ru.ifmo.nds.mnds;

import java.util.Arrays;
import java.util.BitSet;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.ArraySorter;

public final class BitSetImplementation extends NonDominatedSorting {
    private double[][] points;
    private int[] ranks;
    private BitSet[] pointBitSets;
    private BitSet scanBitSet;

    public BitSetImplementation(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        points = new double[maximumPoints][];
        ranks = new int[maximumPoints];
        pointBitSets = new BitSet[maximumPoints];
        scanBitSet = new BitSet(maximumPoints);
        for (int i = 0; i < maximumPoints; ++i) {
            pointBitSets[i] = new BitSet(i);
        }
    }

    @Override
    public String getName() {
        return "MNDS-BitSet";
    }

    @Override
    protected void closeImpl() {
        points = null;
        ranks = null;
        scanBitSet = null;
        pointBitSets = null;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int origN = ranks.length;
        int dim = points[0].length;
        ArrayHelper.fillIdentity(indices, origN);
        sorter.lexicographicalSort(points, indices, 0, origN, dim);
        int newN = ArraySorter.retainUniquePoints(points, indices, this.points, ranks);

        for (int i = 1; i < newN; ++i) {
            pointBitSets[i].set(0, i);
        }
        for (int obj = 1; obj < dim; ++obj) {
            ArrayHelper.fillIdentity(indices, origN);
            sorter.sortComparingByIndicesIfEqual(this.points, indices, 0, newN, obj);
            scanBitSet.clear();
            for (int i = 0; i < newN; ++i) {
                int pointIndex = indices[i];
                pointBitSets[pointIndex].and(scanBitSet);
                scanBitSet.set(pointIndex);
            }
        }
        this.ranks[0] = 0;
        for (int i = 1; i < newN; ++i) {
            BitSet my = pointBitSets[i];
            int myRank = 0;
            for (int index = my.nextSetBit(0); index >= 0; index = my.nextSetBit(index + 1)) {
                int thatRank = this.ranks[index];
                if (thatRank >= myRank) {
                    myRank = thatRank + 1;
                }
            }
            this.ranks[i] = myRank;
        }

        Arrays.fill(this.points, 0, origN, null);
        for (int i = 0; i < origN; ++i) {
            pointBitSets[i].clear();
            ranks[i] = this.ranks[ranks[i]];
        }
    }
}
