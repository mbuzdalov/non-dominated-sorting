package ru.ifmo.nds.domtree;

import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.ArraySorter;
import ru.ifmo.nds.util.DominanceHelper;

public class PresortDelayedNoRecursion extends AbstractDominanceTree {
    private double[][] points;
    private int[] ranks;

    public PresortDelayedNoRecursion(int maximumPoints, int maximumDimension, boolean useRecursiveMerge) {
        super(maximumPoints, maximumDimension, useRecursiveMerge);
        points = new double[maximumPoints][];
        ranks = new int[maximumPoints];
    }

    @Override
    protected void closeImpl() {
        super.closeImpl();
        points = null;
        ranks = null;
    }

    @Override
    public String getName() {
        return "Dominance Tree (presort, "
                + (useRecursiveMerge ? "recursive merge, " : "sequential merge, ")
                + "delayed insertion with sequential concatenation)";
    }

    private static Node concatenate(Node a, Node b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }
        if (a.index > b.index) {
            Node tmp = a;
            a = b;
            b = tmp;
        }
        Node rv = a;
        Node curr = rv;
        a = a.next;
        while (a != null && b != null) {
            if (a.index < b.index) {
                curr.next = a;
                curr = a;
                a = a.next;
            } else {
                curr.next = b;
                curr = b;
                b = b.next;
            }
        }
        curr.next = a != null ? a : b;
        return rv;
    }

    private static Node mergeHelperDelayed(Node main, Node other) {
        Node rv = null;
        double[] mainPoint = main.point;
        int maxObj = mainPoint.length - 1;
        Node concat = null;
        for (Node prev = null, curr = other; curr != null; ) {
            if (DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(mainPoint, curr.point, maxObj)) {
                Node deleted = curr;
                curr = curr.next;
                deleted.next = null;
                concat = concatenate(concat, deleted);
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
        main.child = mergeImpl(main.child, concat);
        return rv;
    }

    private static Node mergeImpl(Node a, Node b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }

        Node rv = null, curr = null;
        while (a != null && b != null) {
            if (a.index < b.index) {
                b = mergeHelperDelayed(a, b);
                Node prevA = a;
                a = a.next;
                prevA.next = null;
                if (curr != null) {
                    curr.next = prevA;
                }
                curr = prevA;
            } else if (a.index > b.index) {
                a = mergeHelperDelayed(b, a);
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
    protected Node merge(Node a, Node b) {
        return mergeImpl(a, b);
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = points.length;
        ArrayHelper.fillIdentity(indices, n);
        sorter.lexicographicalSort(points, indices, 0, n, points[0].length);
        int realN = ArraySorter.retainUniquePoints(points, indices, this.points, ranks);
        sortCheckedImpl(this.points, this.ranks, realN);
        for (int i = 0; i < n; ++i) {
            ranks[i] = this.ranks[ranks[i]];
            this.points[i] = null;
        }
    }
}
