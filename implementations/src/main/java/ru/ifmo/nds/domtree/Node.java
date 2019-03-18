package ru.ifmo.nds.domtree;

import ru.ifmo.nds.util.DominanceHelper;

class Node {
    double[] point;
    Node next, child;
    final int index;

    Node(int index) {
        this.index = index;
    }

    static void initialize(Node node, double[] point) {
        node.point = point;
        node.next = null;
        node.child = null;
    }

    static boolean strictlyDominatesAssumingLexicographicallySmaller(Node node, Node other) {
        double[] p = node.point;
        return DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(p, other.point, p.length - 1);
    }

    static int dominanceComparison(Node node, Node other) {
        double[] p = node.point;
        return DominanceHelper.dominanceComparison(p, other.point, p.length);
    }
}
