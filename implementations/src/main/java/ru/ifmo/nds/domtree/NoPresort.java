package ru.ifmo.nds.domtree;

import ru.ifmo.nds.util.DominanceHelper;

public class NoPresort extends AbstractDominanceTree {
    public NoPresort(int maximumPoints, int maximumDimension, boolean useRecursiveMerge) {
        super(maximumPoints, maximumDimension, useRecursiveMerge);
    }

    @Override
    public String getName() {
        return "Dominance Tree (no presort, " + (useRecursiveMerge ? "recursive merge" : "sequential merge") + ")";
    }

    private static Node mergeImpl(Node a, Node b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
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
                    bCurr.child = mergeImpl(bCurr.child, aDel);
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
                    aCurr.child = mergeImpl(aCurr.child, bDel);
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
    protected Node merge(Node a, Node b) {
        return mergeImpl(a, b);
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        sortCheckedImpl(points, ranks, points.length);
    }
}
