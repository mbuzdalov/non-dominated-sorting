package ru.ifmo.nds.domtree;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.DominanceHelper;

public abstract class AbstractDominanceTree extends NonDominatedSorting {
    private Node[] nodes;
    private Node[] rankMergeArray;
    final boolean useRecursiveMerge;

    AbstractDominanceTree(int maximumPoints, int maximumDimension, boolean useRecursiveMerge) {
        super(maximumPoints, maximumDimension);
        nodes = new Node[maximumPoints];
        rankMergeArray = new Node[maximumPoints];
        this.useRecursiveMerge = useRecursiveMerge;
        for (int i = 0; i < maximumPoints; ++i) {
            nodes[i] = new Node(i);
        }
    }

    @Override
    protected void closeImpl() throws Exception {
        nodes = null;
        rankMergeArray = null;
    }

    void sortCheckedImpl(double[][] points, int[] ranks, int n) {
        for (int i = 0; i < n; ++i) {
            nodes[i].initialize(points[i]);
        }
        Node tree = mergeAllRecursively(nodes, 0, n);
        for (int rank = 0; tree != null; ++rank) {
            int rankMergeCount = 0;
            while (tree != null) {
                ranks[tree.index] = rank;
                if (tree.child != null) {
                    rankMergeArray[rankMergeCount++] = tree.child;
                }
                tree = tree.next;
            }
            tree = mergeAll(rankMergeArray, rankMergeCount);
        }
    }

    protected abstract Node merge(Node a, Node b);

    private Node mergeAll(Node[] array, int size) {
        if (size == 0) {
            return null;
        }
        if (useRecursiveMerge) {
            return mergeAllRecursively(array, 0, size);
        } else {
            Node rv = array[0];
            for (int i = 1; i < size; ++i) {
                rv = merge(rv, array[i]);
            }
            return rv;
        }
    }

    private Node mergeAllRecursively(Node[] array, int from, int until) {
        if (from + 1 == until) {
            return array[from];
        } else {
            int mid = (from + until) >>> 1;
            return merge(mergeAllRecursively(array, from, mid), mergeAllRecursively(array, mid, until));
        }
    }

    static class Node {
        private double[] point;
        Node next, child;
        final int index;

        Node(int index) {
            this.index = index;
            this.next = null;
            this.child = null;
        }

        void initialize(double[] point) {
            this.point = point;
            this.next = null;
            this.child = null;
        }

        boolean dominatesAssumingThisIsNotWorse(Node that) {
            int dim = point.length;
            for (int i = 0; i < dim; ++i) {
                double a = point[i], b = that.point[i];
                if (a > b) {
                    return false;
                }
            }
            return true;
        }

        int dominationCompare(Node that) {
            return DominanceHelper.dominanceComparison(point, that.point);
        }
    }
}
