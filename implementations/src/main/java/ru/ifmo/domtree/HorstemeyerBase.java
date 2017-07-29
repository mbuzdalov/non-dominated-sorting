package ru.ifmo.domtree;

import java.util.ArrayDeque;
import java.util.Queue;

import ru.ifmo.NonDominatedSorting;

public abstract class HorstemeyerBase extends NonDominatedSorting {
    private Node[] nodes;
    private Queue<Tree> trees;

    HorstemeyerBase(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
        nodes = new Node[maximumPoints];
        trees = new ArrayDeque<>();
        for (int i = 0; i < maximumPoints; ++i) {
            nodes[i] = new Node(i);
        }
        for (int i = 0; i < 2 * maximumPoints; ++i) {
            trees.add(new Tree());
        }
    }

    @Override
    protected void closeImpl() throws Exception {
        nodes = null;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks) {
        int n = points.length;
        for (int i = 0; i < n; ++i) {
            nodes[i].reset(points[i], trees.poll());
        }
        Tree root = runDivideConquer(0, n);
        for (int rank = 0; root != null && root.root != null; ++rank) {
            Node curr = root.root;
            root.root = null;
            trees.add(root);
            root = null;
            while (curr != null) {
                ranks[curr.index] = rank;
                root = merge(root, curr.child);
                curr = curr.next;
            }
        }
    }

    private Tree runDivideConquer(int from, int until) {
        if (from + 1 < until) {
            int mid = (from + until) >>> 1;
            Tree l = runDivideConquer(from, mid);
            Tree r = runDivideConquer(mid, until);
            return merge(l, r);
        } else {
            return nodes[from].tree;
        }
    }

    Tree isolate(Node node) {
        if (node.prev != null) {
            node.prev.next = node.next;
        } else {
            node.tree.root = node.next;
        }
        if (node.next != null) {
            node.next.prev = node.prev;
        }
        if (node.tree.root == null) {
            trees.add(node.tree);
        }
        node.prev = node.next = null;
        node.tree = trees.poll();
        node.tree.root = node;
        return node.tree;
    }

    void mergeChild(Node whoseChildToMerge, Tree whatToMerge) {
        whoseChildToMerge.child = merge(whoseChildToMerge.child, whatToMerge);
    }

    protected abstract Tree merge(Tree l, Tree r);

    Tree concatenate(Tree l, Tree r) {
        if (l == null || l.root == null) {
            return r;
        }
        if (r == null || r.root == null) {
            return l;
        }
        Node curr = r.root;
        while (curr != null) {
            if (curr.tree != r) {
                throw new AssertionError();
            }
            Node next = curr.next;
            curr.tree = l;
            if (curr.next == null) {
                curr.next = l.root;
                l.root.prev = curr;
            }
            curr = next;
        }
        l.root = r.root;
        r.root = null;
        trees.add(r);
        return l;
    }

    static class Tree {
        private Node root = null;
        Node root() {
            return root;
        }
    }

    static class Node {
        final int index;
        private double[] point;
        private Node prev = null;
        private Node next = null;
        private Tree child = null;
        private Tree tree = null;

        Node(int index) {
            this.index = index;
        }

        Node next() {
            return next;
        }

        void reset(double[] point, Tree tree) {
            this.point = point;
            this.prev = this.next = null;
            this.child = null;
            this.tree = tree;
            tree.root = this;
        }

        int dominanceCompare(Node that) {
            boolean hasLess = false, hasGreater = false;
            int t = point.length;
            for (int i = 0; i < t; ++i) {
                hasLess |= point[i] < that.point[i];
                hasGreater |= point[i] > that.point[i];
                if (hasLess && hasGreater) {
                    return 0;
                }
            }
            return hasLess ? -1 : hasGreater ? 1 : 0;
        }
    }
}
