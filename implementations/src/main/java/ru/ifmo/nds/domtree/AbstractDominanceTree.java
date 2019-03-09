package ru.ifmo.nds.domtree;

import ru.ifmo.nds.NonDominatedSorting;

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
    protected void closeImpl() {
        nodes = null;
        rankMergeArray = null;
    }

    void sortCheckedImpl(double[][] points, int[] ranks, int n) {
        for (int i = 0; i < n; ++i) {
            nodes[i].initialize(points[i]);
            rankMergeArray[i] = nodes[i];
        }
        Node tree = mergeAllRecursively(0, n);
        for (int rank = 0; tree != null; ++rank) {
            int rankMergeCount = 0;
            while (tree != null) {
                ranks[tree.index] = rank;
                if (tree.child != null) {
                    rankMergeArray[rankMergeCount] = tree.child;
                    ++rankMergeCount;
                }
                tree = tree.next;
            }
            tree = mergeAll(rankMergeCount);
        }
    }

    protected abstract Node merge(Node a, Node b);

    private Node mergeAll(int size) {
        if (size == 0) {
            return null;
        }
        if (useRecursiveMerge) {
            return mergeAllRecursively(0, size);
        } else {
            Node rv = rankMergeArray[0];
            for (int i = 1; i < size; ++i) {
                rv = merge(rv, rankMergeArray[i]);
            }
            return rv;
        }
    }

    private Node mergeAllRecursively(int from, int until) {
        if (from + 1 == until) {
            return rankMergeArray[from];
        } else {
            int mid = (from + until) >>> 1;
            return merge(mergeAllRecursively(from, mid), mergeAllRecursively(mid, until));
        }
    }

    static class Node {
        double[] point;
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
    }
}
