package ru.ifmo.nds.jfb;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.*;

public abstract class JFBBase extends NonDominatedSorting {
    private static final int FORK_JOIN_THRESHOLD = 400;

    // Shared resources (int[] indices from super also belongs here)
    int[] ranks;

    // Data which is immutable throughout the actual sorting.
    private double[][] points;
    double[][] transposedPoints;
    int maximalMeaningfulRank;

    // This is used in preparation phase or in 2D-only sweep.
    private int[] internalIndices;
    private double[] lastFrontOrdinates;

    // Data which is interval-shared between threads.
    private double[] medianSwap;
    private SplitMergeHelper splitMerge;
    private HybridAlgorithmWrapper.Instance hybrid;

    private ForkJoinPool pool;

    private final int allowedThreads;
    private final String nameAddend;

    JFBBase(int maximumPoints,
            int maximumDimension,
            int allowedThreads,
            HybridAlgorithmWrapper hybridWrapper,
            String nameAddend) {
        super(maximumPoints, maximumDimension);
        if (!hybridWrapper.supportsMultipleThreads()) {
            allowedThreads = 1;
        }
        this.nameAddend = nameAddend + ", hybrid: " + hybridWrapper.getName();

        if (allowedThreads != 1 && makesSenseRunInParallel(maximumPoints, maximumDimension)) {
            pool = allowedThreads > 1 ? new ForkJoinPool(allowedThreads) : new ForkJoinPool();
        } else {
            pool = null; // current thread only execution
        }
        this.allowedThreads = allowedThreads > 0 ? allowedThreads : -1;

        medianSwap = new double[maximumPoints];
        ranks = new int[maximumPoints];
        points = new double[maximumPoints][];
        transposedPoints = new double[maximumDimension][maximumPoints];

        internalIndices = new int[maximumPoints];
        lastFrontOrdinates = new double[maximumPoints];
        splitMerge = new SplitMergeHelper(maximumPoints);

        hybrid = hybridWrapper.create(new StateAccessor());
    }

    @Override
    protected void closeImpl() {
        medianSwap = null;
        ranks = null;
        points = null;
        transposedPoints = null;

        internalIndices = null;
        lastFrontOrdinates = null;
        splitMerge = null;

        if (pool != null) {
            pool.shutdown();
            pool = null;
        }

        hybrid = null;
    }

    @Override
    public String getName() {
        return "Jensen-Fortin-Buzdalov, " + getThreadDescription() + ", " + nameAddend;
    }

    @Override
    protected final void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        final int n = points.length;
        final int dim = points[0].length;
        Arrays.fill(ranks, 0);
        ArrayHelper.fillIdentity(internalIndices, n);
        sorter.lexicographicalSort(points, internalIndices, 0, n, dim);

        this.maximalMeaningfulRank = maximalMeaningfulRank;

        if (dim == 2) {
            // 2: Special case: binary search.
            twoDimensionalCase(points, ranks);
        } else {
            // 3: General case.
            // 3.1: Moving points in a sorted order to internal structures
            final int newN = DoubleArraySorter.retainUniquePoints(points, internalIndices, this.points, ranks);
            Arrays.fill(this.ranks, 0, newN, 0);

            // 3.2: Transposing points. This should fit in cache for reasonable dimensions.
            for (int i = 0; i < newN; ++i) {
                for (int j = 0; j < dim; ++j) {
                    transposedPoints[j][i] = this.points[i][j];
                }
            }

            postTransposePointHook(newN);
            ArrayHelper.fillIdentity(indices, newN);

            // 3.3: Calling the actual sorting
            if (pool != null && makesSenseRunInParallel(n, dim)) {
                RecursiveAction action = new RecursiveAction() {
                    @Override
                    protected void compute() {
                        helperA(0, newN, dim - 1);
                    }
                };
                pool.invoke(action);
            } else {
                helperA(0, newN, dim - 1);
            }

            // 3.4: Applying the results back. After that, the argument "ranks" array stops being abused.
            for (int i = 0; i < n; ++i) {
                ranks[i] = this.ranks[ranks[i]];
                this.points[i] = null;
            }
        }
    }

    final int kickOutOverflowedRanks(int from, int until) {
        int newUntil = from;
        for (int i = from; i < until; ++i) {
            int ii = indices[i];
            if (ranks[ii] <= maximalMeaningfulRank) {
                indices[newUntil++] = ii;
            }
        }
        return newUntil;
    }

    private boolean strictlyDominatesAssumingNotSame(int goodIndex, int weakIndex, int maxObj) {
        double[] goodPoint = points[goodIndex];
        double[] weakPoint = points[weakIndex];
        // Comparison in 0 makes no sense, as due to goodIndex < weakIndex the points are <= in this coordinate.
        for (int i = maxObj; i > 0; --i) {
            if (goodPoint[i] > weakPoint[i]) {
                return false;
            }
        }
        return true;
    }

    private boolean tryUpdateRank(int goodIndex, int weakIndex) {
        int rg = ranks[goodIndex];
        if (ranks[weakIndex] <= rg) {
            ranks[weakIndex] = 1 + rg;
            return rg < maximalMeaningfulRank;
        }
        return true;
    }

    protected void postTransposePointHook(int newN) {}

    protected abstract int sweepA(int from, int until);
    protected abstract int sweepB(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int tempFrom);

    private int helperAMain(int from, int until, int obj) {
        ArrayHelper.transplant(transposedPoints[obj], indices, from, until, medianSwap, from);
        double objMin = ArrayHelper.min(medianSwap, from, until);
        double objMax = ArrayHelper.max(medianSwap, from, until);
        if (objMin == objMax) {
            return helperA(from, until, obj - 1);
        } else {
            double median = ArrayHelper.destructiveMedian(medianSwap, from, until);
            long split = splitMerge.splitInThree(transposedPoints[obj], indices,
                    from, from, until, median, objMin, objMax);
            int startMid = SplitMergeHelper.extractMid(split);
            int startRight = SplitMergeHelper.extractRight(split);

            int newStartMid = helperA(from, startMid, obj);
            int newStartRight = helperB(from, newStartMid, startMid, startRight, obj - 1, from);
            newStartRight = helperA(startMid, newStartRight, obj - 1);
            int newUntil = helperB(from, newStartMid, startRight, until, obj - 1, from);
            newUntil = helperB(startMid, newStartRight, startRight, newUntil, obj - 1, from);
            newUntil = helperA(startRight, newUntil, obj);

            return splitMerge.mergeThree(indices, from, from, newStartMid, startMid, newStartRight, startRight, newUntil);
        }
    }

    private boolean ifDominatesUpdateRankAndCheckWhetherCanScrapSecond(int good, int weak, int obj) {
        return strictlyDominatesAssumingNotSame(good, weak, obj) && !tryUpdateRank(good, weak);
    }

    private int helperA(int from, int until, int obj) {
        int n = until - from;
        if (n <= 2) {
            if (n == 2) {
                int goodIndex = indices[from];
                int weakIndex = indices[from + 1];
                if (ifDominatesUpdateRankAndCheckWhetherCanScrapSecond(goodIndex, weakIndex, obj)) {
                    return from + 1;
                }
            }
            return until;
        } else if (obj == 1) {
            return sweepA(from, until);
        } else if (hybrid.helperAHookCondition(until - from, obj)) {
            return hybrid.helperAHook(from, until, obj);
        } else {
            return helperAMain(from, until, obj);
        }
    }

    private int updateByPoint(int pointIndex, int from, int until, int obj) {
        int ri = ranks[pointIndex];
        if (ri == maximalMeaningfulRank) {
            return updateByPointCritical(pointIndex, from, until, obj);
        } else {
            updateByPointNormal(pointIndex, ri, from, until, obj);
            return until;
        }
    }

    private void updateByPointNormal(int pointIndex, int pointRank, int from, int until, int obj) {
        for (int i = from; i < until; ++i) {
            int ii = indices[i];
            if (ranks[ii] <= pointRank && strictlyDominatesAssumingNotSame(pointIndex, ii, obj)) {
                ranks[ii] = pointRank + 1;
            }
        }
    }

    private int updateByPointCritical(int pointIndex, int from, int until, int obj) {
        int minOverflow = until;
        for (int i = from; i < until; ++i) {
            int ii = indices[i];
            if (strictlyDominatesAssumingNotSame(pointIndex, ii, obj)) {
                ranks[ii] = maximalMeaningfulRank + 1;
                if (minOverflow > i) {
                    minOverflow = i;
                }
            }
        }
        return kickOutOverflowedRanks(minOverflow, until);
    }

    private int helperBWeak1(int goodFrom, int goodUntil, int weak, int obj) {
        int wi = indices[weak];
        for (int i = goodFrom; i < goodUntil; ++i) {
            int gi = indices[i];
            if (ifDominatesUpdateRankAndCheckWhetherCanScrapSecond(gi, wi, obj)) {
                return weak;
            }
        }
        return weak + 1;
    }

    private int helperBMain(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom) {
        double[] currentTransposed = transposedPoints[obj];
        int medianGood = ArrayHelper.transplant(currentTransposed, indices, goodFrom, goodUntil, medianSwap, tempFrom);
        double goodMinObj = ArrayHelper.min(medianSwap, tempFrom, medianGood);
        int medianWeak = ArrayHelper.transplant(currentTransposed, indices, weakFrom, weakUntil, medianSwap, medianGood);
        double weakMaxObj = ArrayHelper.max(medianSwap, medianGood, medianWeak);
        if (weakMaxObj < goodMinObj) {
            return weakUntil;
        }
        double goodMaxObj = ArrayHelper.max(medianSwap, tempFrom, medianGood);
        double weakMinObj = ArrayHelper.min(medianSwap, medianGood, medianWeak);
        if (goodMaxObj <= weakMinObj) {
            return helperB(goodFrom, goodUntil, weakFrom, weakUntil, obj - 1, tempFrom);
        }
        double median = ArrayHelper.destructiveMedian(medianSwap, tempFrom, medianWeak);
        long goodSplit = splitMerge.splitInThree(currentTransposed, indices, tempFrom, goodFrom, goodUntil, median, goodMinObj, goodMaxObj);
        int goodMidL = SplitMergeHelper.extractMid(goodSplit);
        int goodMidR = SplitMergeHelper.extractRight(goodSplit);
        long weakSplit = splitMerge.splitInThree(currentTransposed, indices, tempFrom, weakFrom, weakUntil, median, weakMinObj, weakMaxObj);
        int weakMidL = SplitMergeHelper.extractMid(weakSplit);
        int weakMidR = SplitMergeHelper.extractRight(weakSplit);
        int tempMid = tempFrom + ((goodUntil - goodFrom + weakUntil - weakFrom) >>> 1);

        int newWeakUntil = helperB(goodFrom, goodMidL, weakMidR, weakUntil, obj - 1, tempFrom);
        newWeakUntil = helperB(goodMidL, goodMidR, weakMidR, newWeakUntil, obj - 1, tempFrom);

        int newWeakMidR = helperB(goodFrom, goodMidL, weakMidL, weakMidR, obj - 1, tempFrom);
        newWeakMidR = helperB(goodMidL, goodMidR, weakMidL, newWeakMidR, obj - 1, tempFrom);

        ForkJoinTask<Integer> newWeakMidLTask = null;
        if (pool != null && goodMidL - goodFrom + weakMidL - weakFrom > FORK_JOIN_THRESHOLD) {
            newWeakMidLTask = helperBAsync(goodFrom, goodMidL, weakFrom, weakMidL, obj, tempFrom).fork();
        }
        newWeakUntil = helperB(goodMidR, goodUntil, weakMidR, newWeakUntil, obj, tempMid);
        int newWeakMidL = newWeakMidLTask != null
                ? newWeakMidLTask.join()
                : helperB(goodFrom, goodMidL, weakFrom, weakMidL, obj, tempFrom);

        splitMerge.mergeThree(indices, tempFrom, goodFrom, goodMidL, goodMidL, goodMidR, goodMidR, goodUntil);
        return splitMerge.mergeThree(indices, tempFrom,
                weakFrom, newWeakMidL, weakMidL, newWeakMidR, weakMidR, newWeakUntil);
    }

    private RecursiveTask<Integer> helperBAsync(final int goodFrom, final int goodUntil,
                                                final int weakFrom, final int weakUntil,
                                                final int obj,
                                                final int tempFrom) {
        return new RecursiveTask<Integer>() {
            @Override
            protected Integer compute() {
                return helperB(goodFrom, goodUntil, weakFrom, weakUntil, obj, tempFrom);
            }
        };
    }

    private int helperB(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom) {
        if (goodUntil - goodFrom > 0 && weakUntil - weakFrom > 0) {
            int lastWeakIdx = indices[weakUntil - 1];
            while (goodFrom < goodUntil && indices[goodUntil - 1] > lastWeakIdx) {
                --goodUntil;
            }
            int firstGoodIdx = indices[goodFrom];
            while (weakFrom < weakUntil && indices[weakFrom] < firstGoodIdx) {
                ++weakFrom;
            }
        }
        int goodN = goodUntil - goodFrom;
        int weakN = weakUntil - weakFrom;
        if (goodN > 0 && weakN > 0) {
            if (goodN == 1) {
                return updateByPoint(indices[goodFrom], weakFrom, weakUntil, obj);
            } else if (weakN == 1) {
                return helperBWeak1(goodFrom, goodUntil, weakFrom, obj);
            } else if (obj == 1) {
                return sweepB(goodFrom, goodUntil, weakFrom, weakUntil, tempFrom);
            } else if (hybrid.helperBHookCondition(goodFrom, goodUntil, weakFrom, weakUntil, obj)) {
                return hybrid.helperBHook(goodFrom, goodUntil, weakFrom, weakUntil, obj, tempFrom);
            } else {
                return helperBMain(goodFrom, goodUntil, weakFrom, weakUntil, obj, tempFrom);
            }
        } else {
            return weakUntil;
        }
    }

    private void twoDimensionalCase(double[][] points, int[] ranks) {
        // Also uses internalIndices and lastFrontOrdinates
        int maxRank = 1;
        int n = ranks.length;

        double[] firstPoint = points[internalIndices[0]];
        double lastX = firstPoint[0];
        double lastY = firstPoint[1];
        int lastRank = 0;

        // This is used here instead of lastFrontOrdinates[0] to make it slightly faster.
        double minY = lastY;

        for (int i = 1; i < n; ++i) {
            int ii = internalIndices[i];
            double[] pp = points[ii];
            double currX = pp[0];
            double currY = pp[1];

            if (currX == lastX && currY == lastY) {
                // Same point as the previous one.
                // The rank is the same as well.
                ranks[ii] = lastRank;
            } else if (currY < minY) {
                // Y smaller than the smallest Y previously seen.
                // The rank is thus zero.
                // ranks[ii] is already 0.
                minY = currY;
                lastRank = 0;
            } else {
                // At least the Y-smallest point dominates our point.
                int left, right;
                if (currY < lastY) {
                    // We are better than the previous point in Y.
                    // This means that we are at least that good.
                    left = 0;
                    right = lastRank;
                } else {
                    // We are worse (or equal) than the previous point in Y.
                    // This means that we are worse than this point.
                    left = lastRank;
                    right = maxRank;
                }
                // Running the binary search.
                while (right - left > 1) {
                    int mid = (left + right) >>> 1;
                    double midY = lastFrontOrdinates[mid];
                    if (currY < midY) {
                        right = mid;
                    } else {
                        left = mid;
                    }
                }
                // "right" is now our rank.
                ranks[ii] = lastRank = right;
                lastFrontOrdinates[right] = currY;
                if (right == maxRank && maxRank <= maximalMeaningfulRank) {
                    ++maxRank;
                }
            }

            lastX = currX;
            lastY = currY;
        }
    }

    private boolean makesSenseRunInParallel(int nPoints, int dimension) {
        return nPoints > FORK_JOIN_THRESHOLD && dimension > 3;
    }

    private String getThreadDescription() {
        return allowedThreads == -1 ? "unlimited threads" : allowedThreads + " thread(s)";
    }

    public class StateAccessor {
        private StateAccessor() {}

        public final int[] getRanks() {
            return JFBBase.this.ranks;
        }

        public final int[] getIndices() {
            return JFBBase.this.indices;
        }

        public final double[][] getPoints() {
            return JFBBase.this.points;
        }

        public final double[][] getTransposedPoints() {
            return JFBBase.this.transposedPoints;
        }

        public final int getMaximalMeaningfulRank() {
            return JFBBase.this.maximalMeaningfulRank;
        }

        public final int kickOutOverflowedRanks(int from, int until) {
            return JFBBase.this.kickOutOverflowedRanks(from, until);
        }

        public final boolean strictlyDominatesAssumingNotSame(int goodIndex, int weakIndex, int maxObj) {
            return JFBBase.this.strictlyDominatesAssumingNotSame(goodIndex, weakIndex, maxObj);
        }

        public final int updateByPoint(int pointIndex, int from, int until, int obj) {
            return JFBBase.this.updateByPoint(pointIndex, from, until, obj);
        }
    }
}
