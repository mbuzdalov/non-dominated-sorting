package ru.ifmo.nds.domtree;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.ArraySorter;
import ru.ifmo.nds.util.DominanceHelper;

public final class PresortNoDelayed extends NonDominatedSorting {
    private Node[] nodes;
    private Node[] rankMergeArray;
    private final boolean useRecursiveMerge;
    private double[][] points;
    private int[] ranks;

    public PresortNoDelayed(int maximumPoints, int maximumDimension, boolean useRecursiveMerge) {
        super(maximumPoints, maximumDimension);
        nodes = new Node[maximumPoints];
        rankMergeArray = new Node[maximumPoints];
        this.useRecursiveMerge = useRecursiveMerge;
        for (int i = 0; i < maximumPoints; ++i) {
            nodes[i] = new Node(i);
        }
        points = new double[maximumPoints][];
        ranks = new int[maximumPoints];
    }

    @Override
    protected void closeImpl() {
        points = null;
        ranks = null;
        nodes = null;
        rankMergeArray = null;
    }

    @Override
    public String getName() {
        return "Dominance Tree (presort, "
                + (useRecursiveMerge ? "recursive merge, " : "sequential merge, ")
                + "no delayed insertion)";
    }

    private static Node mergeHelperNoDelayed(Node main, Node other) {
        Node rv = null;
        double[] mainPoint = main.point;
        int maxObj = mainPoint.length - 1;
        for (Node prev = null, curr = other; curr != null; ) {
            if (DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(mainPoint, curr.point, maxObj)) {
                Node deleted = curr;
                curr = curr.next;
                deleted.next = null;
                main.child = merge(main.child, deleted);
                if (prev != null) {
                    prev.next = curr;
                }
            } else {
                prev = curr;
                curr = curr.next;
            }
            if (prev != null && rv == null) {
                rv = prev;
            }
        }
        return rv;
    }

    private static Node merge(Node a, Node b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }

        Node rv = null, curr = null;
        while (a != null && b != null) {
            if (a.index < b.index) {
                b = mergeHelperNoDelayed(a, b);
                Node prevA = a;
                a = a.next;
                prevA.next = null;
                if (curr != null) {
                    curr.next = prevA;
                }
                curr = prevA;
            } else {
                a = mergeHelperNoDelayed(b, a);
                Node prevB = b;
                b = b.next;
                prevB.next = null;
                if (curr != null) {
                    curr.next = prevB;
                }
                curr = prevB;
            }
            if (rv == null) {
                rv = curr;
            }
        }
        curr.next = a != null ? a : b;
        return rv;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = points.length;
        ArrayHelper.fillIdentity(indices, n);
        sorter.lexicographicalSort(points, indices, 0, n, points[0].length);
        int realN = ArraySorter.retainUniquePoints(points, indices, this.points, ranks);

        for (int i = 0; i < realN; ++i) {
            nodes[i].initialize(this.points[i]);
        }
        Node tree = mergeAllRecursively(nodes, 0, realN);
        for (int rank = 0; tree != null; ++rank) {
            int rankMergeCount = rankAndPutChildrenToMergeArray(tree, this.ranks, rank);
            tree = mergeAll(rankMergeArray, rankMergeCount, useRecursiveMerge);
        }

        for (int i = 0; i < n; ++i) {
            ranks[i] = this.ranks[ranks[i]];
            this.points[i] = null;
        }
    }

    private int rankAndPutChildrenToMergeArray(Node tree, int[] ranks, int rank) {
        int rankMergeCount = 0;
        while (tree != null) {
            ranks[tree.index] = rank;
            if (tree.child != null) {
                rankMergeArray[rankMergeCount] = tree.child;
                ++rankMergeCount;
            }
            tree = tree.next;
        }
        return rankMergeCount;
    }

    private static Node mergeAll(Node[] array, int size, boolean useRecursiveMerge) {
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

    private static Node mergeAllRecursively(Node[] array, int from, int until) {
        if (from + 1 == until) {
            return array[from];
        } else {
            int mid = (from + until) >>> 1;
            return merge(mergeAllRecursively(array, from, mid), mergeAllRecursively(array, mid, until));
        }
    }
}
