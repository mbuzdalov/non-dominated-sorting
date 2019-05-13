package ru.ifmo.nds.ndt;

import ru.ifmo.nds.util.DominanceHelper;

public abstract class TreeRankNode {
    public static final class RankEvaluationContext {
        public double[] point;
        public int rank;
        public int maxObj;
        public int operations;
    }

    public abstract TreeRankNode add(double[] point, int rank, Split split, int splitThreshold);

    public abstract void evaluateRank(RankEvaluationContext ctx, Split split);

    protected abstract int getMaxRank();

    public static final TreeRankNode EMPTY = new EmptyRankNode();
    public static final TreeRankNode EMPTY_1 = new EmptyRankNode1();

    private static class EmptyRankNode extends TreeRankNode {
        @Override
        public TreeRankNode add(double[] point, int rank, Split split, int splitThreshold) {
            return new TerminalRankNode().add(point, rank, split, splitThreshold);
        }

        @Override
        public void evaluateRank(RankEvaluationContext ctx, Split split) {}

        @Override
        protected int getMaxRank() {
            return -1;
        }
    }

    private static class EmptyRankNode1 extends TreeRankNode {
        @Override
        public TreeRankNode add(double[] point, int rank, Split split, int splitThreshold) {
            return new TerminalRankNode1().add(point, rank, split, splitThreshold);
        }

        @Override
        public void evaluateRank(RankEvaluationContext ctx, Split split) {}

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
                return new BranchingRankNode(good, this).add(point, rank, split, splitThreshold);
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
        public void evaluateRank(RankEvaluationContext ctx, Split split) {
            if (maxRank < ctx.rank) {
                return;
            }
            for (int i = size - 1; i >= 0; --i) {
                ++ctx.operations;
                if (ranks[i] < ctx.rank) {
                    return;
                }
                if (DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(points[i], ctx.point, ctx.maxObj)) {
                    ctx.rank = ranks[i] + 1;
                    return;
                }
            }
        }

        @Override
        protected int getMaxRank() {
            return maxRank;
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
                TerminalRankNode1 good = new TerminalRankNode1();
                Split goodSplit = split.good;
                int obj = split.coordinate;
                double median = split.value;
                if (this.point[obj] < median) {
                    good.add(this.point, this.rank, goodSplit, splitThreshold);
                    this.point = null;
                    this.rank = -1;
                }
                return new BranchingRankNode(good, this).add(point, rank, split, splitThreshold);
            } else {
                this.point = point;
                this.rank = rank;
                return this;
            }
        }

        @Override
        public void evaluateRank(RankEvaluationContext ctx, Split split) {
            ++ctx.operations;
            if (this.rank >= ctx.rank &&
                    DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(this.point, ctx.point, ctx.maxObj)) {
                ctx.rank = this.rank + 1;
            }
        }

        @Override
        protected int getMaxRank() {
            return rank;
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
        public void evaluateRank(RankEvaluationContext ctx, Split split) {
            ++ctx.operations;
            if (maxRank < ctx.rank) {
                return;
            }
            if (weak != null && ctx.point[split.coordinate] >= split.value) {
                weak.evaluateRank(ctx, split.weak);
            }
            if (good != null) {
                good.evaluateRank(ctx, split.good);
            }
        }

        @Override
        protected int getMaxRank() {
            return maxRank;
        }
    }
}
