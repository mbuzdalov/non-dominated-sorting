package ru.ifmo.nds.domtree;

import ru.ifmo.nds.util.DominanceHelper;

class Node implements Comparable<Node> {
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

    boolean strictlyDominatesAssumingLexicographicallySmaller(Node other) {
        return DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(point, other.point, point.length - 1);
    }

    int dominanceComparison(Node other) {
        return DominanceHelper.dominanceComparison(point, other.point, point.length);
    }

    @Override
    public int compareTo(Node o) {
        return index - o.index;
    }
}
