package ru.ifmo.domtree;

import ru.ifmo.NonDominatedSorting;

import java.util.ArrayDeque;
import java.util.Queue;

public class NoDelayedInsertion extends NonDominatedSorting {
    private Node[] nodes;
    private Queue<Tree> trees;

    public NoDelayedInsertion(int maximumPoints, int maximumDimension) {
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
    public String getName() {
        return "Dominance Tree (no delayed insertion)";
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

    private Tree merge(Tree l, Tree r) {
        if (l == null || l.root == null) {
            return r;
        }
        if (r == null || r.root == null) {
            return l;
        }
        for (Node left = l.root; left != null; ) {
            boolean leftIsDominated = false;
            for (Node right = r.root; right != null; ) {
                int comparison = left.dominanceCompare(right);
                if (comparison < 0) {
                    Node newRight = right.next;
                    Tree rightTree = right.removeMe(trees);
                    right = newRight;
                    left.child = merge(left.child, rightTree);
                } else if (comparison > 0) {
                    Node newLeft = left.next;
                    Tree leftTree = left.removeMe(trees);
                    left = newLeft;
                    right.child = merge(right.child, leftTree);
                    leftIsDominated = true;
                    break;
                } else {
                    right = right.next;
                }
            }
            if (!leftIsDominated) {
                left = left.next;
            }
        }
        l = concatenate(l, r, trees);
        return l;
    }

    private Tree concatenate(Tree l, Tree r, Queue<Tree> trees) {
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

    private static class Tree {
        Node root = null;
    }

    private static class Node {
        final int index;
        double[] point;
        Node prev = null;
        Node next = null;
        Tree child = null;
        Tree tree = null;

        Node(int index) {
            this.index = index;
        }

        void reset(double[] point, Tree tree) {
            this.point = point;
            this.prev = this.next = null;
            this.child = null;
            this.tree = tree;
            tree.root = this;
        }

        Tree removeMe(Queue<Tree> treeQueue) {
            if (prev != null) {
                prev.next = next;
            } else {
                tree.root = next;
            }
            if (next != null) {
                next.prev = prev;
            }
            if (tree.root == null) {
                treeQueue.add(tree);
            }
            prev = next = null;
            tree = treeQueue.poll();
            tree.root = this;
            return tree;
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
