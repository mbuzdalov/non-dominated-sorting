package ru.ifmo.nds.ndt;

import java.util.Arrays;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DoubleArraySorter;

public class ENS_NDT_Arrays extends NonDominatedSorting {
    private static final int THRESHOLD = 3;

    private DoubleArraySorter sorter;
    private SplitBuilder splitBuilder;
    private int[] indices;
    private int[] ranks;
    private double[][] transposedPoints;
    private double[][] points;

    private int[] nodeArray;
    private int nNodes;
    private int nSlots;

    public ENS_NDT_Arrays(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        this.sorter = new DoubleArraySorter(maximumPoints);
        this.splitBuilder = new SplitBuilder(maximumPoints);
        this.indices = new int[maximumPoints * THRESHOLD];
        this.ranks = new int[maximumPoints];
        this.transposedPoints = new double[maximumDimension][maximumPoints];
        this.points = new double[maximumPoints][];
        // TODO: I still wonder where 6 comes from. This means there are 3 * N nodes in the tree.
        this.nodeArray = new int[maximumPoints * 6];
    }

    @Override
    public String getName() {
        return "ENS-NDT (Arrays)";
    }

    @Override
    protected void closeImpl() {
        sorter = null;
        splitBuilder = null;
        indices = null;
        ranks = null;
        transposedPoints = null;
        points = null;
        nodeArray = null;
    }

    private void add(int node, int index, Split split) {
        double[] point = points[index];
        while (true) {
            int n2 = node * 2, n21 = node * 2 + 1;
            int size = nodeArray[n2];
            if (size >= THRESHOLD) {
                int obj = split.coordinate;
                double med = split.value;
                int goodStartIndex = nodeArray[n21];
                int weakStartIndex = THRESHOLD * nSlots++;
                int goodSize = 0, weakSize = 0;
                for (int i = 0; i < size; ++i) {
                    int current = indices[goodStartIndex + i];
                    if (points[current][obj] >= med) {
                        indices[weakStartIndex + weakSize++] = current;
                    } else {
                        indices[goodStartIndex + goodSize++] = current;
                    }
                }
                int goodNode = nNodes++;
                int weakNode = nNodes++;
                nodeArray[2 * goodNode] = goodSize;
                nodeArray[2 * goodNode + 1] = goodStartIndex;
                nodeArray[2 * weakNode] = weakSize;
                nodeArray[2 * weakNode + 1] = weakStartIndex;
                nodeArray[n2] = -goodNode - 1;
                nodeArray[n21] = -weakNode - 1;
                size = -1;
            }
            if (size >= 0) {
                if (nodeArray[n21] == -1) {
                    nodeArray[n21] = THRESHOLD * nSlots++;
                }
                indices[nodeArray[n21] + nodeArray[n2]++] = index;
                break;
            } else {
                if (point[split.coordinate] >= split.value) {
                    node = -nodeArray[n21] - 1;
                    split = split.weak;
                } else {
                    node = -nodeArray[n2] - 1;
                    split = split.good;
                }
            }
        }
    }

    private boolean dominates(int good, int weak) {
        double[] goodPoint = points[good];
        double[] weakPoint = points[weak];
        // objective 0 is not compared since points are presorted.
        for (int o = goodPoint.length - 1; o > 0; --o) {
            if (goodPoint[o] > weakPoint[o]) {
                return false;
            }
        }
        return true;
    }

    private boolean dominates(int node, int index, Split split) {
        int size = nodeArray[2 * node];
        if (size >= 0) {
            // Terminal node
            int startIndex = nodeArray[2 * node + 1];
            for (int i = 0; i < size; ++i) {
                if (dominates(indices[startIndex + i], index)) {
                    return true;
                }
            }
            return false;
        } else {
            // Branching node
            int goodNode = -size - 1;
            if (goodNode != -1 && dominates(goodNode, index, split.good)) {
                return true;
            }
            int weakNode = -nodeArray[2 * node + 1] - 1;
            return weakNode != -1 && points[index][split.coordinate] >= split.value
                    && dominates(weakNode, index, split.weak);
        }
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = points.length;
        int dim = points[0].length;
        ArrayHelper.fillIdentity(indices, n);
        sorter.lexicographicalSort(points, indices, 0, n, points[0].length);
        int newN = DoubleArraySorter.retainUniquePoints(points, indices, this.points, ranks);
        Arrays.fill(this.ranks, 0, newN, 0);

        for (int i = 0; i < newN; ++i) {
            nodeArray[2 * i] = 0;
            nodeArray[2 * i + 1] = -1;
            for (int j = 0; j < dim; ++j) {
                transposedPoints[j][i] = this.points[i][j];
            }
        }

        Split split = splitBuilder.result(transposedPoints, newN, dim, THRESHOLD);

        int maxRank = 1;
        nNodes = newN;
        nSlots = 0;
        add(0, 0, split);
        for (int i = 1; i < newN; ++i) {
            if (dominates(0, i, split)) {
                int left = 0, right = maxRank;
                while (right - left > 1) {
                    int mid = (left + right) >>> 1;
                    if (dominates(mid, i, split)) {
                        left = mid;
                    } else {
                        right = mid;
                    }
                }
                int rank = left + 1;
                this.ranks[i] = rank;
                if (rank <= maximalMeaningfulRank) {
                    add(rank, i, split);
                    if (rank == maxRank) {
                        ++maxRank;
                    }
                }
            } else {
                add(0, i, split);
            }
        }

        for (int i = 0; i < n; ++i) {
            ranks[i] = this.ranks[ranks[i]];
            this.points[i] = null;
        }
    }
}
