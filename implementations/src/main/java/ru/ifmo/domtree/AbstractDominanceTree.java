package ru.ifmo.domtree;

import ru.ifmo.NonDominatedSorting;

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
        Node tree = buildTree(0, n);
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
             Node rv = array[0];
             for (int i = 1; i < size; ++i) {
                 rv = merge(rv, array[i]);
             }
             return rv;
        } else {
            return mergeAllRecursively(array, 0, size);
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

    private Node buildTree(int from, int until) {
        if (from + 1 == until) {
            return nodes[from];
        } else {
            int mid = (from + until) >>> 1;
            return merge(buildTree(from, mid), buildTree(mid, until));
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

        int dominationCompare(Node that) {
            int dim = point.length;
            boolean less = false, greater = false;
            for (int i = 0; i < dim; ++i) {
                less |= point[i] < that.point[i];
                greater |= point[i] > that.point[i];
                if (less && greater) {
                    return 0;
                }
            }
            return less ? -1 : greater ? 1 : 0;
        }
    }
}
