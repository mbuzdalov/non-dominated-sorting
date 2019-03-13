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

    // assumes b != null && b.next == null
    static Node concatenate(Node a, Node b) {
        if (a == null) {
            return b;
        }
        int bIndex = b.index;
        if (a.index > bIndex) {
            b.next = a;
            return b;
        }
        Node prev = a, next = a.next;
        while (next != null) {
            if (next.index > bIndex) {
                b.next = next;
                break;
            }
            prev = next;
            next = next.next;
        }
        prev.next = b;
        return a;
    }

    @Override
    public int compareTo(Node o) {
        return index - o.index;
    }
}
