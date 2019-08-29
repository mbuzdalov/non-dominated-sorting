package ru.ifmo.nds.ndt;

import ru.ifmo.nds.util.DominanceHelper;

public abstract class TreeRankNode {
    public abstract TreeRankNode add(double[] point, int rank, Split split, int splitThreshold);

    public abstract int evaluateRank(double[] point, int rank, Split split, int maxObj);

    public static final TreeRankNode EMPTY = new EmptyRankNode();
    public static final TreeRankNode EMPTY_1 = new EmptyRankNode1();

    private static class EmptyRankNode extends TreeRankNode {
        @Override
        public TreeRankNode add(double[] point, int rank, Split split, int splitThreshold) {
            return new TerminalRankNode().add(point, rank, split, splitThreshold);
        }

        @Override
        public int evaluateRank(double[] point, int rank, Split split, int maxObj) {
            return rank;
        }
    }

    private static class EmptyRankNode1 extends TreeRankNode {
        @Override
        public TreeRankNode add(double[] point, int rank, Split split, int splitThreshold) {
            return new TerminalRankNode1().add(point, rank, split, splitThreshold);
        }

        @Override
        public int evaluateRank(double[] point, int rank, Split split, int maxObj) {
            return rank;
        }
    }

    private static class TerminalRankNode extends TreeRankNode {
        private int size;
        private double[][] points;
        private int[] ranks;
        int maxRank;

        private TerminalRankNode() {
            this.points = null;
            this.size = 0;
            this.ranks = null;
        }

        @Override
        public TreeRankNode add(double[] point, int rank, Split split, int splitThreshold) {
            if (split == Split.NULL_MAX_DEPTH) {
                if (points == null) {
                    points = new double[1][];
                    ranks = new int[1];
                }
                points[0] = point;
                maxRank = Math.max(maxRank, rank);
                ranks[0] = maxRank;
                size = 1;
                return this;
            }

            if (points == null) {
                points = new double[splitThreshold][];
                ranks = new int[splitThreshold];
            }

            if (size == points.length) {
                int oldMaxRank = maxRank;
                TerminalRankNode good = new TerminalRankNode();
                Split weakSplit = split.weak, goodSplit = split.good;
                int obj = split.coordinate;
                double median = split.value;
                int oldSize = size;
                maxRank = 0;
                size = 0;
                if (weakSplit == Split.NULL_MAX_DEPTH) {
                    double[] p0 = null;
                    for (int i = 0; i < oldSize; ++i) {
                        double[] pi = points[i];
                        int ri = ranks[i];
                        points[i] = null;
                        if (pi[obj] < median) {
                            good.add(pi, ri, goodSplit, splitThreshold);
                        } else {
                            size = 1;
                            p0 = pi;
                            maxRank = Math.max(maxRank, ri);
                        }
                    }
                    points[0] = p0;
                    ranks[0] = maxRank;
                } else {
                    for (int i = 0; i < oldSize; ++i) {
                        double[] pi = points[i];
                        int ri = ranks[i];
                        points[i] = null;
                        if (pi[obj] < median) {
                            good.add(pi, ri, goodSplit, splitThreshold);
                        } else {
                            points[size] = pi;
                            ranks[size] = ri;
                            maxRank = Math.max(maxRank, ri);
                            ++size;
                        }
                    }
                }
                return new BranchingRankNode(good, this, oldMaxRank).add(point, rank, split, splitThreshold);
            } else {
                for (int i = size; i >= 0; --i) {
                    if (i == 0 || ranks[i - 1] <= rank) {
                        points[i] = point;
                        ranks[i] = rank;
                        break;
                    } else {
                        points[i] = points[i - 1];
                        ranks[i] = ranks[i - 1];
                    }
                }
                ++size;
                maxRank = Math.max(maxRank, rank);

                return this;
            }
        }

        @Override
        public int evaluateRank(double[] point, int rank, Split split, int maxObj) {
            if (maxRank < rank) {
                return rank;
            }
            for (int i = size - 1; i >= 0; --i) {
                if (ranks[i] < rank) {
                    return rank;
                }
                if (DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(points[i], point, maxObj)) {
                    return ranks[i] + 1;
                }
            }
            return rank;
        }
    }

    private static class TerminalRankNode1 extends TreeRankNode {
        private double[] point;
        private int rank;

        private TerminalRankNode1() {
            this.point = null;
            this.rank = -1;
        }

        @Override
        public TreeRankNode add(double[] point, int rank, Split split, int splitThreshold) {
            if (split == Split.NULL_MAX_DEPTH) {
                this.point = point;
                this.rank = Math.max(this.rank, rank);
                return this;
            }

            if (this.point != null) {
                int oldRank = this.rank;
                TerminalRankNode1 good = new TerminalRankNode1();
                Split goodSplit = split.good;
                int obj = split.coordinate;
                double median = split.value;
                if (this.point[obj] < median) {
                    good.add(this.point, this.rank, goodSplit, splitThreshold);
                    this.point = null;
                    this.rank = -1;
                }
                return new BranchingRankNode(good, this, oldRank).add(point, rank, split, splitThreshold);
            } else {
                this.point = point;
                this.rank = rank;
                return this;
            }
        }

        @Override
        public int evaluateRank(double[] point, int rank, Split split, int maxObj) {
            if (this.rank >= rank && DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(this.point, point, maxObj)) {
                return this.rank + 1;
            }
            return rank;
        }
    }

    private static class BranchingRankNode extends TreeRankNode {
        private TreeRankNode good, weak;
        private int maxRank;

        private BranchingRankNode(TreeRankNode good, TreeRankNode weak, int maxRank) {
            this.weak = weak;
            this.good = good;
            this.maxRank = maxRank;
        }

        @Override
        public TreeRankNode add(double[] point, int rank, Split split, int splitThreshold) {
            maxRank = Math.max(rank, maxRank);
            if (point[split.coordinate] >= split.value) {
                weak = weak.add(point, rank, split.weak, splitThreshold);
            } else {
                good = good.add(point, rank, split.good, splitThreshold);
            }
            return this;
        }

        @Override
        public int evaluateRank(double[] point, int rank, Split split, int maxObj) {
            if (maxRank < rank) {
                return rank;
            }
            if (weak != null && point[split.coordinate] >= split.value) {
                rank = weak.evaluateRank(point, rank, split.weak, maxObj);
            }
            if (good != null) {
                rank = good.evaluateRank(point, rank, split.good, maxObj);
            }
            return rank;
        }
    }
}
