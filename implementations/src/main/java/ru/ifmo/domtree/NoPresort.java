package ru.ifmo.domtree;

public class NoPresort extends AbstractDominanceTree {
    public NoPresort(int maximumPoints, int maximumDimension, boolean useRecursiveMerge) {
        super(maximumPoints, maximumDimension, useRecursiveMerge);
    }

    @Override
    public String getName() {
        return "Dominance Tree (no presort, " + (useRecursiveMerge ? "recursive merge" : "sequential merge") + ")";
    }

    @Override
    protected Node merge(Node a, Node b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        for (Node aPrev = null, aCurr = a; aCurr != null; ) {
            boolean aRemoved = false;
            for (Node bPrev = null, bCurr = b; bCurr != null; ) {
                int compare = aCurr.dominationCompare(bCurr);
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
        return concatenate(a, b);
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        sortCheckedImpl(points, ranks, points.length);
    }

    private Node concatenate(Node source, Node append) {
        if (source == null) {
            return append;
        } else if (append == null) {
            return source;
        } else {
            Node tmp = source;
            while (tmp.next != null) {
                tmp = tmp.next;
            }
            tmp.next = append;
            return source;
        }
    }
}
