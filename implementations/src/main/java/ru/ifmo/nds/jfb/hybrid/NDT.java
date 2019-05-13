package ru.ifmo.nds.jfb.hybrid;

import ru.ifmo.nds.jfb.HybridAlgorithmWrapper;
import ru.ifmo.nds.jfb.JFBBase;
import ru.ifmo.nds.jfb.hybrid.tuning.Threshold;
import ru.ifmo.nds.jfb.hybrid.tuning.ThresholdFactory;
import ru.ifmo.nds.ndt.Split;
import ru.ifmo.nds.ndt.SplitBuilder;
import ru.ifmo.nds.ndt.TreeRankNode;

public final class NDT extends HybridAlgorithmWrapper {
    private final ThresholdFactory threshold3D;
    private final ThresholdFactory thresholdAll;
    private final int treeThreshold;

    public NDT(ThresholdFactory threshold3D, ThresholdFactory thresholdAll, int treeThreshold) {
        this.threshold3D = threshold3D;
        this.thresholdAll = thresholdAll;
        this.treeThreshold = treeThreshold;
    }

    @Override
    public boolean supportsMultipleThreads() {
        return true;
    }

    @Override
    public String getName() {
        return "NDT (threshold 3D = " + threshold3D.getDescription()
                + ", threshold all = " + thresholdAll.getDescription()
                + ", tree threshold = " + treeThreshold + ")";
    }

    @Override
    public HybridAlgorithmWrapper.Instance create(int[] ranks, int[] indices, double[][] points, double[][] transposedPoints) {
        return new Instance(ranks, indices, points, transposedPoints, threshold3D, thresholdAll, treeThreshold);
    }

    private static final ThreadLocal<TreeRankNode.RankEvaluationContext> contexts =
            ThreadLocal.withInitial(TreeRankNode.RankEvaluationContext::new);

    private static final double[] A_IN_OPS = {
            4.685313180923034, // for d = 2
            5.062883623321451,
            3.503631720739254,
            2.2736727297546144,
            1.630742389166701,
            1.4495154879556986,
            1.434241823252567,
            1.4032441834788256
    };

    private static final double[] P_IN_OPS = {
            0.015332333045967268, // for d = 2
            0.14316676164960723,
            0.26411624362740815,
            0.3564856546604639,
            0.4162172410288698,
            0.4382815729645708,
            0.4428886704739746,
            0.44701314145948956
    };

    private static int computeBudget(int problemSize, int objective) {
        // Notes on performance counting on some fixed laptop.
        // For helperB in NDT hybrid:
        //     for x operations, the time is roughly 16 x nanoseconds.
        // For helperB in divide-and-conquer:
        //     for n points and objective d, the time is estimated, in nanoseconds, as
        //        b_d + a_d * n * pow(n, p_d) * log(n + 1)
        //     where:
        //        d = 2:  a_2 = 49.488620473499545, b_2 =  -424.3548303036347, p_2 = 0.015332333045967268
        //        d = 3:  a_3 = 53.476708271332825, b_3 = -4301.263427341121,  p_3 = 0.14316676164960723
        //        d = 4:  a_4 = 37.00711005030837,  b_4 = -5604.850673951447,  p_4 = 0.26411624362740815
        //        d = 5:  a_5 = 24.015668208033116, b_5 = -3510.8851507558597, p_5 = 0.3564856546604639
        //        d = 6:  a_6 = 17.22471648557328,  b_6 =  -323.7417409726593, p_6 = 0.4162172410288698
        //        d = 7:  a_7 = 15.310507341532068, b_7 =  1389.9330709265287, p_7 = 0.4382815729645708
        //        d = 8:  a_8 = 15.14917925810524,  b_8 =  1498.2703347533609, p_8 = 0.4428886704739746
        //        d = 9+: a_9 = 14.821766687995096, b_9 =  1732.0452197266432, p_9 = 0.44701314145948956
        // Hence the arrays above have been computed.

        objective = Math.min(objective - 2, 7);
        double estimation = A_IN_OPS[objective] * problemSize * Math.pow(problemSize, P_IN_OPS[objective]) * Math.log(1 + problemSize);
        return (int) (estimation * 0.3);
    }

    private static final class Instance extends HybridAlgorithmWrapper.Instance {
        private SplitBuilder splitBuilder;

        private double[][] points;
        private int[] indices;
        private int[] ranks;

        private double[][] localPoints;

        private final Threshold[] thresholds;
        private final int threshold;

        private Instance(int[] ranks, int[] indices, double[][] points, double[][] transposedPoints,
                         ThresholdFactory threshold3D, ThresholdFactory thresholdAll, int treeThreshold) {
            this.ranks = ranks;
            this.indices = indices;
            this.points = points;

            thresholds = new Threshold[8];
            for (int i = 0; i < thresholds.length; ++i) {
                thresholds[i] = (i == 0 ? threshold3D : thresholdAll).createThreshold();
            }

            this.threshold = treeThreshold;

            int maximumPoints = indices.length;
            int maximumDimension = transposedPoints.length;
            this.splitBuilder = new SplitBuilder(transposedPoints, maximumPoints, threshold);
            this.localPoints = new double[maximumPoints][maximumDimension];
        }

        private boolean notHookCondition(int size, int obj) {
            return obj == 1 || size >= thresholds[Math.min(obj - 2, 7)].getThreshold();
        }

        @Override
        public int helperAHook(int from, int until, int obj, int maximalMeaningfulRank) {
            int problemSize = until - from;
            if (notHookCondition(problemSize, obj)) {
                return -1;
            }

            Split split = splitBuilder.result(from, until, indices, obj + 1);
            Threshold sizeThreshold = thresholds[Math.min(obj - 2, 7)];
            int budget = computeBudget(problemSize, obj);

            for (int i = from; i < until; ++i) {
                System.arraycopy(points[indices[i]], 1, localPoints[i], 1, obj);
            }

            TreeRankNode.RankEvaluationContext ctx = contexts.get();
            ctx.operations = 0;
            ctx.maxObj = obj;

            int minOverflow = until;
            TreeRankNode tree = threshold == 1 ? TreeRankNode.EMPTY_1 : TreeRankNode.EMPTY;
            for (int i = from; i < until; ++i) {
                int idx = indices[i];
                ctx.rank = ranks[idx];
                ctx.point = localPoints[i];
                tree.evaluateRank(ctx, split);
                ranks[idx] = ctx.rank;

                if (ranks[idx] <= maximalMeaningfulRank) {
                    tree = tree.add(localPoints[i], ranks[idx], split, threshold);
                } else if (minOverflow > i) {
                    minOverflow = i;
                }
                if (sizeThreshold.shallTerminate(budget, ctx.operations)) {
                    sizeThreshold.recordPerformance(problemSize, budget, ctx.operations, true);
                    return -1;
                }
            }
            sizeThreshold.recordPerformance(problemSize, budget, ctx.operations, false);
            return JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minOverflow, until);
        }

        @Override
        public int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom, int maximalMeaningfulRank) {
            int problemSize = goodUntil - goodFrom + weakUntil - weakFrom;
            if (notHookCondition(problemSize, obj)) {
                return -1;
            }

            Split split = splitBuilder.result(goodFrom, goodUntil, indices, obj + 1);
            Threshold sizeThreshold = thresholds[Math.min(obj - 2, 7)];
            int budget = computeBudget(problemSize, obj);

            for (int good = goodFrom; good < goodUntil; ++good) {
                System.arraycopy(points[indices[good]], 1, localPoints[good], 1, obj);
            }
            for (int weak = weakFrom; weak < weakUntil; ++weak) {
                System.arraycopy(points[indices[weak]], 1, localPoints[weak], 1, obj);
            }

            TreeRankNode.RankEvaluationContext ctx = contexts.get();
            ctx.operations = 0;
            ctx.maxObj = obj;

            int minOverflow = weakUntil;
            TreeRankNode tree = threshold == 1 ? TreeRankNode.EMPTY_1 : TreeRankNode.EMPTY;
            for (int good = goodFrom, weak = weakFrom; weak < weakUntil; ++weak) {
                int wi = indices[weak];
                int gi;
                while (good < goodUntil && (gi = indices[good]) < wi) {
                    tree = tree.add(localPoints[good], ranks[gi], split, threshold);
                    ++good;
                }
                ctx.rank = ranks[wi];
                ctx.point = localPoints[weak];
                tree.evaluateRank(ctx, split);
                ranks[wi] = ctx.rank;
                if (minOverflow > weak && ranks[wi] > maximalMeaningfulRank) {
                    minOverflow = weak;
                }
                if (sizeThreshold.shallTerminate(budget, ctx.operations)) {
                    sizeThreshold.recordPerformance(problemSize, budget, ctx.operations, true);
                    return -1;
                }
            }
            sizeThreshold.recordPerformance(problemSize, budget, ctx.operations, false);
            return JFBBase.kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minOverflow, weakUntil);
        }
    }
}
