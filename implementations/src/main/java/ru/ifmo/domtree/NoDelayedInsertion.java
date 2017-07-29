package ru.ifmo.domtree;

public class NoDelayedInsertion extends HorstemeyerBase {
    public NoDelayedInsertion(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    public String getName() {
        return "Dominance Tree (no delayed insertion)";
    }

    @Override
    protected Tree merge(Tree l, Tree r) {
        if (l == null || l.root() == null) {
            return r;
        }
        if (r == null || r.root() == null) {
            return l;
        }
        for (Node left = l.root(); left != null; ) {
            boolean leftIsDominated = false;
            for (Node right = r.root(); right != null; ) {
                int comparison = left.dominanceCompare(right);
                if (comparison < 0) {
                    Node newRight = right.next();
                    Tree rightTree = isolate(right);
                    right = newRight;
                    mergeChild(left, rightTree);
                } else if (comparison > 0) {
                    Node newLeft = left.next();
                    Tree leftTree = isolate(left);
                    left = newLeft;
                    mergeChild(right, leftTree);
                    leftIsDominated = true;
                    break;
                } else {
                    right = right.next();
                }
            }
            if (!leftIsDominated) {
                left = left.next();
            }
        }
        l = concatenate(l, r);
        return l;
    }
}
