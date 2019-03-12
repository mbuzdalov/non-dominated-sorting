package ru.ifmo.nds.ndt;

import ru.ifmo.nds.util.DominanceHelper;

public abstract class TreeRankNode {
    public abstract TreeRankNode add(double[] point, int rank, Split split, int splitThreshold);

    public abstract int evaluateRank(double[] point, int rank, Split split, int maxObj);

    protected abstract int getMaxRank();

    public static final TreeRankNode EMPTY = new EmptyRankNode();

    private static class EmptyRankNode extends TreeRankNode {
        @Override
        public TreeRankNode add(double[] point, int rank, Split split, int splitThreshold) {
            return new TerminalRankNode().add(point, rank, split, splitThreshold);
        }

        @Override
        public int evaluateRank(double[] point, int rank, Split split, int maxObj) {
            return rank;
        }

        @Override
        protected int getMaxRank() {
            return -1;
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
                    size = 1;
                }
                points[0] = point;
                maxRank = Math.max(maxRank, rank);
                ranks[0] = maxRank;
                return this;
            }

            if (points == null) {
                points = new double[splitThreshold][];
                ranks = new int[splitThreshold];
            }

            if (size == points.length) {
                TerminalRankNode weak = new TerminalRankNode();
                TerminalRankNode good = new TerminalRankNode();
                // actually, nulls are perfect here,
                // but we will not hurt the hearts of those who suffered from NPE
                Split weakSplit = split.weak, goodSplit = split.good;
                int obj = split.coordinate;
                double median = split.value;
                for (int i = 0; i < size; ++i) {
                    if (points[i][obj] < median) {
                        good.add(points[i], ranks[i], goodSplit, splitThreshold);
                    } else {
                        weak.add(points[i], ranks[i], weakSplit, splitThreshold);
                    }
                }
                TreeRankNode rv = new BranchingRankNode(good, weak);
                return rv.add(point, rank, split, splitThreshold);
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

        @Override
        protected int getMaxRank() {
            return maxRank;
        }
    }

    private static class BranchingRankNode extends TreeRankNode {
        private TreeRankNode good, weak;
        private int maxRank;

        private BranchingRankNode(TreeRankNode good, TreeRankNode weak) {
            this.weak = weak;
            this.good = good;
            this.maxRank = Math.max(good.getMaxRank(), weak.getMaxRank());
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

        @Override
        protected int getMaxRank() {
            return maxRank;
        }
    }
}
