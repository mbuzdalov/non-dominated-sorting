package ru.ifmo.domtree;

import ru.ifmo.NonDominatedSorting;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Queue;

public class Horstemeyer2008 extends NonDominatedSorting {
    private Node[] nodes;
    private Queue<Tree> trees;

    public Horstemeyer2008(int maximumPoints, int maximumDimension) {
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
        return "Dominance Tree (Horstemeyer 2008 from ECJ)";
    }

    @Override
    protected void closeImpl() throws Exception {
        nodes = null;
    }

    @Override
    protected void sortChecked(double[][] points, int[] ranks) {
        int n = points.length;
        int oldSize = trees.size();
        for (int i = 0; i < n; ++i) {
            nodes[i].reset(points[i], trees.poll());
        }
        Tree root = runDivideConquer(0, n);
        traverse(root, 0, ranks);
        if (oldSize != trees.size()) {
            throw new AssertionError();
        }
    }

    private void traverse(Tree tree, int rank, int[] ranks) {
        for (Node curr = tree.root; curr != null; curr = curr.next) {
            if (curr.tree != tree) {
                throw new AssertionError();
            }
            curr.tree = null;
            ranks[curr.index] = rank;
            if (curr.child != null) {
                traverse(curr.child, rank + 1, ranks);
            }
        }
        tree.root = null;
        trees.add(tree);
    }

    private Tree runDivideConquer(int from, int until) {
        if (from + 1 < until) {
            int mid = (from + until) >>> 1;
            Tree l = runDivideConquer(from, mid);
            Tree r = runDivideConquer(mid, until);
            l.check();
            r.check();
            Tree rv = merge(l, r);
            System.out.println("Merge at [" + from + "; " + until + "):");
            rv.printout(1);
            return rv;
        } else {
            Tree rv = nodes[from].tree;
            System.out.println("Merge at [" + from + "; " + until + "):");
            rv.printout(1);
            return rv;
        }
    }

    private Tree merge(Tree l, Tree r) {
        if (l == null || l.root == null) {
            return r;
        }
        if (r == null || r.root == null) {
            return l;
        }
        l.check();
        r.check();
        Node left = l.root, right = r.root;
        while (left != null && right != null) {
            int comparison = left.dominanceCompare(right);
            if (comparison < 0) {
                Node newRight = right.next;
                Tree rightTree = right.removeMe(trees);
                right = newRight;
                left.child = merge(left.child, rightTree);
                left.child.check();
            } else if (comparison > 0) {
                Node newLeft = left.next;
                Tree leftTree = left.removeMe(trees);
                left = newLeft;
                right.child = merge(right.child, leftTree);
                right.child.check();
            } else {
                right = right.next;
                if (right == null) {
                    left = left.next;
                    right = r.root;
                }
            }
        }
        l = concatenate(l, r, trees);
        l.check();
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

        void check() {
            for (Node n = root; n != null; n = n.next) {
                if (n.tree != this) throw new AssertionError();
                if (n == root && n.prev != null) throw new AssertionError();
                if (n != root && n.prev == null) throw new AssertionError();
                if (n != root && n.prev.next != n) throw new AssertionError();
                if (n.next != null && n.next.prev != n) throw new AssertionError();
            }
        }

        void printout(int depth) {
            Node curr = root;
            while (curr != null) {
                for (int i = 0; i < depth; ++i) {
                    System.out.print("    ");
                }
                System.out.println(Arrays.toString(curr.point));
                if (curr.child != null) {
                    curr.child.printout(depth + 1);
                }
                curr = curr.next;
            }
        }
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
            tree.check();
            if (tree.root == null) {
                treeQueue.add(tree);
            }
            prev = next = null;
            tree = treeQueue.poll();
            tree.root = this;
            tree.check();
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
