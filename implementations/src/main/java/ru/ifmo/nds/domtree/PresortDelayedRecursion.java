package ru.ifmo.nds.domtree;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.ArraySorter;
import ru.ifmo.nds.util.DominanceHelper;

public final class PresortDelayedRecursion extends NonDominatedSorting {
    private Node[] nodes;
    private Node[] rankMergeArray;
    private final boolean useRecursiveMerge;
    private Node[] concatenationNodes;
    private double[][] points;
    private int[] ranks;

    public PresortDelayedRecursion(int maximumPoints, int maximumDimension, boolean useRecursiveMerge) {
        super(maximumPoints, maximumDimension);
        nodes = new Node[maximumPoints];
        rankMergeArray = new Node[maximumPoints];
        this.useRecursiveMerge = useRecursiveMerge;
        for (int i = 0; i < maximumPoints; ++i) {
            nodes[i] = new Node(i);
        }
        concatenationNodes = new Node[maximumPoints];
        points = new double[maximumPoints][];
        ranks = new int[maximumPoints];
    }

    @Override
    protected void closeImpl() {
        nodes = null;
        rankMergeArray = null;
        points = null;
        ranks = null;
        concatenationNodes = null;
    }

    @Override
    public String getName() {
        return "Dominance Tree (presort, "
                + (useRecursiveMerge ? "recursive merge, " : "sequential merge, ")
                + "delayed insertion with recursive concatenation)";
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

    private static Node concatenateRecursively(Node[] array, int from, int until) {
        if (from + 1 == until) {
            return array[from];
        } else {
            int mid = (from + until) >>> 1;
            return concatenate(concatenateRecursively(array, from, mid), concatenateRecursively(array, mid, until));
        }
    }

    private static Node mergeHelperDelayed(Node main, Node other, Node[] tmp) {
        Node rv = null;
        int concatCount = 0;
        double[] mainPoint = main.point;
        int maxObj = mainPoint.length - 1;
        for (Node prev = null, curr = other; curr != null; ) {
            if (DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(mainPoint, curr.point, maxObj)) {
                Node deleted = curr;
                curr = curr.next;
                deleted.next = null;
                tmp[concatCount] = deleted;
                ++concatCount;
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
        if (concatCount > 0) {
            main.child = merge(main.child, concatenateRecursively(tmp, 0, concatCount), tmp);
        }
        return rv;
    }

    private static Node merge(Node a, Node b, Node[] tmp) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }

        Node rv = null, curr = null;
        while (a != null && b != null) {
            if (a.index < b.index) {
                b = mergeHelperDelayed(a, b, tmp);
                Node prevA = a;
                a = a.next;
                prevA.next = null;
                if (curr != null) {
                    curr.next = prevA;
                }
                curr = prevA;
            } else {
                a = mergeHelperDelayed(b, a, tmp);
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

        Node tree = mergeAllRecursively(nodes, concatenationNodes, 0, realN);
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

    private Node mergeAll(Node[] array, int size, boolean useRecursiveMerge) {
        if (size == 0) {
            return null;
        }
        if (useRecursiveMerge) {
            return mergeAllRecursively(array, concatenationNodes, 0, size);
        } else {
            Node rv = array[0];
            for (int i = 1; i < size; ++i) {
                rv = merge(rv, array[i], concatenationNodes);
            }
            return rv;
        }
    }

    private static Node mergeAllRecursively(Node[] array, Node[] concat, int from, int until) {
        if (from + 1 == until) {
            return array[from];
        } else {
            int mid = (from + until) >>> 1;
            return merge(mergeAllRecursively(array, concat, from, mid), mergeAllRecursively(array, concat, mid, until), concat);
        }
    }
}
