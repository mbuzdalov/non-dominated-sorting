package ru.ifmo.nds.domtree;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.DominanceHelper;

public final class NoPresort extends NonDominatedSorting {
    private Node[] nodes;
    private Node[] rankMergeArray;
    private final boolean useRecursiveMerge;

    public NoPresort(int maximumPoints, int maximumDimension, boolean useRecursiveMerge) {
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

    @Override
    public String getName() {
        return "Dominance Tree (no presort, " + (useRecursiveMerge ? "recursive merge" : "sequential merge") + ")";
    }

    private static Node merge(Node a, Node b) {
        if (a == null) {
            return b;
        }

        assert b != null;

        Node aPrev = null;
        for (Node aCurr = a; aCurr != null; ) {
            double[] aPoint = aCurr.point;
            int aLength = aPoint.length;
            boolean aRemoved = false;
            for (Node bPrev = null, bCurr = b; bCurr != null; ) {
                int compare = DominanceHelper.dominanceComparison(aPoint, bCurr.point, aLength);
                if (compare > 0) {
                    Node aDel = aCurr;
                    aCurr = aCurr.next;
                    if (aPrev == null) {
                        a = aCurr;
                    } else {
                        aPrev.next = aCurr;
                    }
                    aDel.next = null;
                    bCurr.child = merge(bCurr.child, aDel);
                    aRemoved = true;
                    break;
                } else if (compare < 0) {
                    Node bDel = bCurr;
                    bCurr = bCurr.next;
                    if (bPrev == null) {
                        b = bCurr;
                    } else {
                        bPrev.next = bCurr;
                    }
                    bDel.next = null;
                    aCurr.child = merge(aCurr.child, bDel);
                } else {
                    bPrev = bCurr;
                    bCurr = bCurr.next;
                }
            }
            if (!aRemoved) {
                aPrev = aCurr;
                aCurr = aCurr.next;
            }
        }
        if (a == null) {
            return b;
        } else {
            aPrev.next = b;
            return a;
        }
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = points.length;
        for (int i = 0; i < n; ++i) {
            nodes[i].initialize(points[i]);
        }
        Node tree = mergeAllRecursively(nodes, 0, n);
        for (int rank = 0; tree != null; ++rank) {
            int rankMergeCount = rankAndPutChildrenToMergeArray(tree, ranks, rank);
            tree = mergeAll(rankMergeArray, rankMergeCount, useRecursiveMerge);
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
