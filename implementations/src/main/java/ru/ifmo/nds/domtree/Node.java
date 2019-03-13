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

    static Node concatenate(Node a, Node b) {
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

    @Override
    public int compareTo(Node o) {
        return index - o.index;
    }
}
