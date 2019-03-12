package ru.ifmo.nds.ndt;

import ru.ifmo.nds.util.DominanceHelper;

public abstract class TreeNode {
    public abstract TreeNode add(double[] point, Split split, int splitThreshold);
    public abstract boolean dominates(double[] point, Split split);

    static final TreeNode EMPTY = new EmptyNode();
    static final TreeNode EMPTY_1 = new EmptyNode1();

    private static class EmptyNode extends TreeNode {
        @Override
        public TreeNode add(double[] point, Split split, int splitThreshold) {
            return new TerminalNode().add(point, split, splitThreshold);
        }

        @Override
        public boolean dominates(double[] point, Split split) {
            return false;
        }
    }

    private static class EmptyNode1 extends TreeNode {
        @Override
        public TreeNode add(double[] point, Split split, int splitThreshold) {
            return new TerminalNode1().add(point, split, splitThreshold);
        }

        @Override
        public boolean dominates(double[] point, Split split) {
            return false;
        }
    }

    private static class TerminalNode extends TreeNode {
        private int size;
        private double[][] points;

        private TerminalNode() {
            this.points = null;
            this.size = 0;
        }

        @Override
        public TreeNode add(double[] point, Split split, int splitThreshold) {
            if (points == null) {
                points = new double[splitThreshold][];
            }
            if (size == points.length) {
                TerminalNode good = new TerminalNode();
                Split goodSplit = split.good;
                int obj = split.coordinate;
                double median = split.value;
                int oldSize = size;
                size = 0;
                for (int i = 0; i < oldSize; ++i) {
                    double[] pi = points[i];
                    if (pi[obj] < median) {
                        good.add(pi, goodSplit, splitThreshold);
                    } else {
                        points[size] = pi;
                        ++size;
                    }
                }
                return new BranchingNode(good, this).add(point, split, splitThreshold);
            } else {
                points[size++] = point;
                return this;
            }
        }

        @Override
        public boolean dominates(double[] point, Split split) {
            int maxObj = point.length - 1;
            for (int i = 0; i < size; ++i) {
                if (DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(points[i], point, maxObj)) {
                    return true;
                }
            }
            return false;
        }
    }

    private static class TerminalNode1 extends TreeNode {
        private double[] point;

        private TerminalNode1() {
            this.point = null;
        }

        @Override
        public TreeNode add(double[] point, Split split, int splitThreshold) {
            if (this.point != null) {
                TerminalNode1 good = new TerminalNode1();
                Split goodSplit = split.good;
                int obj = split.coordinate;
                double median = split.value;
                if (this.point[obj] < median) {
                    good.add(this.point, goodSplit, splitThreshold);
                    this.point = null;
                }
                return new BranchingNode(good, this).add(point, split, splitThreshold);
            } else {
                this.point = point;
                return this;
            }
        }

        @Override
        public boolean dominates(double[] point, Split split) {
            return this.point != null &&
                    DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(this.point, point, point.length - 1);
        }
    }

    private static class BranchingNode extends TreeNode {
        private TreeNode good, weak;

        private BranchingNode(TreeNode good, TreeNode weak) {
            this.weak = weak;
            this.good = good;
        }

        @Override
        public TreeNode add(double[] point, Split split, int splitThreshold) {
            if (point[split.coordinate] >= split.value) {
                weak = weak.add(point, split.weak, splitThreshold);
            } else {
                good = good.add(point, split.good, splitThreshold);
            }
            return this;
        }

        @Override
        public boolean dominates(double[] point, Split split) {
            return good != null && good.dominates(point, split.good) ||
                    weak != null && point[split.coordinate] >= split.value && weak.dominates(point, split.weak);
        }
    }
}
