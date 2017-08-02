package ru.ifmo.domtree;

import ru.ifmo.util.DoubleArraySorter;

import static ru.ifmo.DominanceTree.InsertionOption;

public class Presort extends AbstractDominanceTree {
    private Node[] concatenationNodes;
    private double[][] points;
    private int[] ranks;
    private DoubleArraySorter sorter;
    private final InsertionOption insertionOption;

    public Presort(int maximumPoints, int maximumDimension, boolean useRecursiveMerge, InsertionOption insertionOption) {
        super(maximumPoints, maximumDimension, useRecursiveMerge);
        this.insertionOption = insertionOption;
        points = new double[maximumPoints][];
        ranks = new int[maximumPoints];
        sorter = new DoubleArraySorter(maximumPoints);
        concatenationNodes = new Node[maximumPoints];
    }

    @Override
    protected void closeImpl() throws Exception {
        super.closeImpl();
        points = null;
        ranks = null;
        sorter = null;
        concatenationNodes = null;
    }

    @Override
    public String getName() {
        return "Dominance Tree (presort, "
                + (useRecursiveMerge ? "recursive merge, " : "sequential merge, ")
                + insertionOption.humanReadableDescription() + ")";
    }

    private Node concatenate(Node a, Node b) {
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
        if (a != null) {
            curr.next = a;
        } else {
            curr.next = b;
        }
        return rv;
    }

    private Node concatenateRecursively(int from, int until) {
        if (from + 1 == until) {
            return concatenationNodes[from];
        } else {
            int mid = (from + until) >>> 1;
            return concatenate(concatenateRecursively(from, mid), concatenateRecursively(mid, until));
        }
    }

    private Node concatenateAll(int count) {
        if (count == 0) {
            return null;
        }
        if (insertionOption == InsertionOption.DELAYED_INSERTION_RECURSIVE_CONCATENATION) {
            return concatenateRecursively(0, count);
        } else if (insertionOption == InsertionOption.DELAYED_INSERTION_SEQUENTIAL_CONCATENATION) {
            Node rv = concatenationNodes[0];
            concatenationNodes[0] = null;
            for (int i = 1; i < count; ++i) {
                rv = concatenate(rv, concatenationNodes[i]);
                concatenationNodes[i] = null;
            }
            return rv;
        } else {
            throw new AssertionError("concatenateAll called with insertion option " + insertionOption);
        }
    }

    private Node mergeHelperNoDelayed(Node main, Node other) {
        Node rv = null;
        for (Node prev = null, curr = other; curr != null; ) {
            int comparison = main.dominationCompare(curr);
            if (comparison > 0) {
                throw new AssertionError();
            }
            if (comparison < 0) {
                Node deleted = curr;
                curr = curr.next;
                deleted.next = null;
                main.child = merge(main.child, deleted);
                if (main.child == main) {
                    throw new AssertionError();
                }
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

    private Node mergeHelperDelayed(Node main, Node other) {
        Node rv = null;
        int concatCount = 0;
        for (Node prev = null, curr = other; curr != null; ) {
            int comparison = main.dominationCompare(curr);
            if (comparison < 0) {
                Node deleted = curr;
                curr = curr.next;
                deleted.next = null;
                concatenationNodes[concatCount++] = deleted;
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
        main.child = merge(main.child, concatenateAll(concatCount));
        return rv;
    }

    private Node mergeHelper(Node main, Node other) {
        if (insertionOption == InsertionOption.NO_DELAYED_INSERTION) {
            return mergeHelperNoDelayed(main, other);
        } else {
            return mergeHelperDelayed(main, other);
        }
    }

    @Override
    protected Node merge(Node a, Node b) {
        if (a == null) {
            return b;
        }
        if (b == null) {
            return a;
        }

        Node rv = null, curr = null;
        while (a != null && b != null) {
            if (a.index < b.index) {
                b = mergeHelper(a, b);
                Node prevA = a;
                a = a.next;
                prevA.next = null;
                if (curr != null) {
                    curr.next = prevA;
                }
                curr = prevA;
            } else if (a.index > b.index) {
                a = mergeHelper(b, a);
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
        if (a != null) {
            curr.next = a;
        } else {
            curr.next = b;
        }
        return rv;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = points.length;
        // this.ranks are temporarily abused to mean indices
        for (int i = 0; i < n; ++i) {
            this.ranks[i] = i;
        }
        sorter.lexicographicalSort(points, this.ranks, 0, n, points[0].length);
        int realN = DoubleArraySorter.retainUniquePoints(points, this.ranks, this.points, ranks);
        // from this point on, this.ranks stop being abused, but arg ranks stores reindexing.
        sortCheckedImpl(this.points, this.ranks, realN);
        for (int i = 0; i < n; ++i) {
            ranks[i] = this.ranks[ranks[i]];
        }
    }
}
