package ru.ifmo.nds.ndt;

public abstract class TreeRankNode {
    public abstract TreeRankNode add(double[] point, int rank, Split split, int splitThreshold);

    public abstract int evaluateRank(double[] point, int rank, Split split, int M);

    public abstract int getMaxRank();

    public static final TreeRankNode EMPTY = new EmptyRankNode();

    private static class EmptyRankNode extends TreeRankNode {
        @Override
        public TreeRankNode add(double[] point, int rank, Split split, int splitThreshold) {
            return new TerminalRankNode().add(point, rank, split, splitThreshold);
        }

        @Override
        public int evaluateRank(double[] point, int rank, Split split, int M) {
            return rank;
        }

        @Override
        public int getMaxRank() {
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
            if (points == null) {
                points = new double[splitThreshold][];
                ranks = new int[splitThreshold];
            }
            if (size == points.length) {
                if (split == null) {
                    // если мы находимся в терминальной в смысле split вершине, то bucket size всегда = 1
                    // то есть будем работаеть только с points[0] и ranks[0]
                    // TODO почему нельзя выше?

                    // TODO оптимизация нужна ?
                    points[0] = point;
                    ranks[0] = Math.max(ranks[0], rank);
                    maxRank = Math.max(maxRank, rank);
                    size = 1;

//                    points[size - 1] = point;
//                    ranks[size - 1] = Math.max(ranks[size - 1], rank);
//                    maxRank = Math.max(maxRank, rank);

                    return this;
                }
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
                points[size] = point;
                ranks[size] = rank;
                size++;
                maxRank = Math.max(maxRank, rank);

                return this;
            }
        }

        @Override
        public int evaluateRank(double[] point, int rank, Split split, int M) {
            if (getMaxRank() < rank) {
                return rank;
            }

            pointLoop:
            for (int i = 0; i < size; ++i) { // TODO возможно надо поменять порядок обхода
                double[] current = points[i];
                if (this.ranks[i] + 1 < rank) {
                    continue;
                }
                // objective 0 is not compared since points are presorted.

                for (int o = M - 1; o > 0; --o) {
                    if (current[o] > point[o]) {
                        continue pointLoop;
                    }
                }

                rank = Math.max(this.ranks[i] + 1, rank);
            }
            return rank;
        }

        @Override
        public int getMaxRank() {
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
        public int evaluateRank(double[] point, int rank, Split split, int M) {
            if (getMaxRank() < rank) {
                return rank;
            }

            int resultRank = -1;
            if (weak != null && point[split.coordinate] >= split.value) {
                resultRank = weak.evaluateRank(point, rank, split.weak, M);
            }

            if (good != null) {
                resultRank = good.evaluateRank(point, Math.max(resultRank, rank), split.good, M);
            }

            return Math.max(resultRank, rank);
        }

        @Override
        public int getMaxRank() {
            return maxRank;
        }
    }
}
