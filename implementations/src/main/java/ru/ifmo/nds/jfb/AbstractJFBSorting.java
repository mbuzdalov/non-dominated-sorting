package ru.ifmo.nds.jfb;

import java.util.Arrays;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.RecursiveTask;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DoubleArraySorter;
import ru.ifmo.nds.util.RankQueryStructure;
import ru.ifmo.nds.util.SplitMergeHelper;

public abstract class AbstractJFBSorting extends NonDominatedSorting {
    private static final int FORK_JOIN_THRESHOLD = 400;

    // Shared resources
    int[] indices;
    int[] ranks;

    // Data which is immutable throughout the actual sorting.
    private double[][] points;
    private double[][] transposedPoints;
    int maximalMeaningfulRank;

    // This is used in preparation phase or in 2D-only sweep.
    private DoubleArraySorter sorter;
    private int[] internalIndices;
    private double[] lastFrontOrdinates;

    // Data which is interval-shared between threads.
    private double[] medianSwap;
    private RankQueryStructure rankQuery;
    private SplitMergeHelper splitMerge;

    private ForkJoinPool pool;

    private final int allowedThreads;

    AbstractJFBSorting(int maximumPoints, int maximumDimension, int allowedThreads) {
        super(maximumPoints, maximumDimension);

        if (allowedThreads == 1) {
            pool = null; // current thread only execution
        } else {
            pool = allowedThreads > 1 ? new ForkJoinPool(allowedThreads) : new ForkJoinPool();
        }
        this.allowedThreads = allowedThreads > 0 ? allowedThreads : -1;

        sorter = new DoubleArraySorter(maximumPoints);
        medianSwap = new double[maximumPoints];
        indices = new int[maximumPoints];
        ranks = new int[maximumPoints];
        points = new double[maximumPoints][];
        transposedPoints = new double[maximumDimension][maximumPoints];
        rankQuery = createStructure(maximumPoints);

        internalIndices = new int[maximumPoints];
        lastFrontOrdinates = new double[maximumPoints];
        splitMerge = new SplitMergeHelper(maximumPoints);
    }

    @Override
    protected void closeImpl() {
        sorter = null;
        medianSwap = null;
        indices = null;
        ranks = null;
        points = null;
        rankQuery = null;
        transposedPoints = null;

        internalIndices = null;
        lastFrontOrdinates = null;

        if (pool != null) {
            pool.shutdown();
            pool = null;
        }
    }

    @Override
    protected final void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        final int n = points.length;
        final int dim = points[0].length;
        Arrays.fill(ranks, 0);
        ArrayHelper.fillIdentity(internalIndices, n);
        sorter.lexicographicalSort(points, internalIndices, 0, n, dim);

        this.maximalMeaningfulRank = maximalMeaningfulRank;

        if (dim == 1) {
            // 1: This is equivalent to ordinary sorting.
            for (int i = 0, r = 0; i < n; ++i) {
                int ii = internalIndices[i];
                ranks[ii] = r;
                if (i + 1 < n && points[ii][0] != points[internalIndices[i + 1]][0]) {
                    ++r;
                }
            }
        } else if (dim == 2) {
            // 2: Special case: binary search.
            twoDimensionalCase(points, ranks);
        } else {
            // 3: General case.
            // 3.1: Moving points in a sorted order to internal structures
            final int newN = DoubleArraySorter.retainUniquePoints(points, internalIndices, this.points, ranks);
            Arrays.fill(this.ranks, 0, newN, 0);
            ArrayHelper.fillIdentity(this.indices, newN);

            // 3.2: Transposing points. This should fit in cache for reasonable dimensions.
            for (int i = 0; i < newN; ++i) {
                for (int j = 0; j < dim; ++j) {
                    transposedPoints[j][i] = this.points[i][j];
                }
            }

            // 3.3: Calling the actual sorting
            if (pool == null) {
                helperA(0, newN, dim - 1);
            } else {
                RecursiveAction action = new RecursiveAction() {
                    @Override
                    protected void compute() {
                        helperA(0, newN, dim - 1);
                    }
                };
                pool.invoke(action);
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

    final boolean strictlyDominatesAssumingNotSame(int goodIndex, int weakIndex, int maxObj) {
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

    protected abstract RankQueryStructure createStructure(int maximumPoints);

    private int sweepA(int from, int until) {
        double[] local = transposedPoints[1];
        RankQueryStructure.RangeHandle rankQuery = this.rankQuery.createHandle(from, until);

        if (rankQuery.needsPossibleKeys()) {
            for (int i = from; i < until; ++i) {
                rankQuery.addPossibleKey(local[indices[i]]);
            }
        }
        rankQuery.init();
        int minOverflow = until;
        for (int i = from; i < until; ++i) {
            int curr = indices[i];
            double currY = local[curr];
            int result = Math.max(ranks[curr],
                    rankQuery.getMaximumWithKeyAtMost(currY, ranks[curr]) + 1);
            ranks[curr] = result;
            if (result <= maximalMeaningfulRank) {
                rankQuery.put(currY, result);
            } else if (minOverflow > i) {
                minOverflow = i;
            }
        }
        rankQuery.clear();
        return kickOutOverflowedRanks(minOverflow, until);
    }

    private int sweepB(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int tempFrom, int tempUntil) {
        double[] local = transposedPoints[1];
        RankQueryStructure.RangeHandle rankQuery = this.rankQuery.createHandle(tempFrom, tempUntil);

        if (rankQuery.needsPossibleKeys()) {
            for (int i = goodFrom; i < goodUntil; ++i) {
                rankQuery.addPossibleKey(local[indices[i]]);
            }
        }
        rankQuery.init();
        int goodI = goodFrom;
        int minOverflow = weakUntil;
        for (int weakI = weakFrom; weakI < weakUntil; ++weakI) {
            int weakCurr = indices[weakI];
            while (goodI < goodUntil && indices[goodI] < weakCurr) {
                int goodCurr = indices[goodI++];
                rankQuery.put(local[goodCurr], ranks[goodCurr]);
            }
            int result = Math.max(ranks[weakCurr],
                    rankQuery.getMaximumWithKeyAtMost(local[weakCurr], ranks[weakCurr]) + 1);
            ranks[weakCurr] = result;
            if (minOverflow > weakI && result > maximalMeaningfulRank) {
                minOverflow = weakI;
            }
        }
        rankQuery.clear();
        return kickOutOverflowedRanks(minOverflow, weakUntil);
    }

    protected boolean helperAHookCondition(int size, int obj) {
        return false;
    }
    protected int helperAHook(int from, int until, int obj) {
        throw new UnsupportedOperationException("helperAHook not yet implemented");
    }

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
            int newStartRight = helperB(from, newStartMid, startMid, startRight, obj - 1, from, until);
            newStartRight = helperA(startMid, newStartRight, obj - 1);
            newStartRight = splitMerge.mergeTwo(indices, from, from, newStartMid, startMid, newStartRight);
            int newUntil = helperB(from, newStartRight, startRight, until, obj - 1, from, until);
            newUntil = helperA(startRight, newUntil, obj);
            return splitMerge.mergeTwo(indices, from, from, newStartRight, startRight, newUntil);
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
        } else if (helperAHookCondition(until - from, obj)) {
            return helperAHook(from, until, obj);
        } else {
            return helperAMain(from, until, obj);
        }
    }

    final int updateByPoint(int pointIndex, int from, int until, int obj) {
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

    private int helperBGood1(int good, int weakFrom, int weakUntil, int obj) {
        int gi = indices[good];
        // Binary search to discard points which are definitely not dominated.
        int bs = Arrays.binarySearch(indices, weakFrom, weakUntil, gi);
        int weakStart = -bs - 1;
        return updateByPoint(gi, weakStart, weakUntil, obj);
    }

    private int helperBWeak1(int goodFrom, int goodUntil, int weak, int obj) {
        int wi = indices[weak];
        // Binary search to discard points which definitely do not dominate.
        int bs = Arrays.binarySearch(indices, goodFrom, goodUntil, wi);
        int goodFinish = -bs - 1;
        for (int i = goodFrom; i < goodFinish; ++i) {
            int gi = indices[i];
            if (ifDominatesUpdateRankAndCheckWhetherCanScrapSecond(gi, wi, obj)) {
                return weak;
            }
        }
        return weak + 1;
    }

    protected boolean helperBHookCondition(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        return false;
    }
    protected int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom, int tempUntil) {
        throw new UnsupportedOperationException("helperBHook not yet implemented");
    }

    private int helperBMain(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom, int tempUntil) {
        int medianGood = ArrayHelper.transplant(transposedPoints[obj], indices, goodFrom, goodUntil, medianSwap, tempFrom);
        double goodMaxObj = ArrayHelper.max(medianSwap, tempFrom, medianGood);
        int medianWeak = ArrayHelper.transplant(transposedPoints[obj], indices, weakFrom, weakUntil, medianSwap, medianGood);
        double weakMinObj = ArrayHelper.min(medianSwap, medianGood, medianWeak);
        if (goodMaxObj <= weakMinObj) {
            return helperB(goodFrom, goodUntil, weakFrom, weakUntil, obj - 1, tempFrom, tempUntil);
        } else {
            double goodMinObj = ArrayHelper.min(medianSwap, tempFrom, medianGood);
            double weakMaxObj = ArrayHelper.max(medianSwap, medianGood, medianWeak);
            double median = ArrayHelper.destructiveMedian(medianSwap, tempFrom, medianWeak);
            long goodSplit = splitMerge.splitInThree(transposedPoints[obj], indices, tempFrom, goodFrom, goodUntil, median, goodMinObj, goodMaxObj);
            int goodMidL = SplitMergeHelper.extractMid(goodSplit);
            int goodMidR = SplitMergeHelper.extractRight(goodSplit);
            long weakSplit = splitMerge.splitInThree(transposedPoints[obj], indices, tempFrom, weakFrom, weakUntil, median, weakMinObj, weakMaxObj);
            int weakMidL = SplitMergeHelper.extractMid(weakSplit);
            int weakMidR = SplitMergeHelper.extractRight(weakSplit);
            int tempMid = (tempFrom + tempUntil) >>> 1;

            ForkJoinTask<Integer> newWeakMidLTask = null;
            if (pool != null && goodMidL - goodFrom + weakMidL - weakFrom > FORK_JOIN_THRESHOLD) {
                newWeakMidLTask = helperBAsync(goodFrom, goodMidL, weakFrom, weakMidL, obj, tempFrom, tempMid).fork();
            }
            int newWeakUntil = helperB(goodMidR, goodUntil, weakMidR, weakUntil, obj, tempMid, tempUntil);
            int newWeakMidL = newWeakMidLTask != null
                    ? newWeakMidLTask.join()
                    : helperB(goodFrom, goodMidL, weakFrom, weakMidL, obj, tempFrom, tempMid);

            splitMerge.mergeTwo(indices, tempFrom, goodFrom, goodMidL, goodMidL, goodMidR);
            newWeakUntil = splitMerge.mergeTwo(indices, tempFrom, weakMidL, weakMidR, weakMidR, newWeakUntil);
            newWeakUntil = helperB(goodFrom, goodMidR, weakMidL, newWeakUntil, obj - 1, tempFrom, tempUntil);
            splitMerge.mergeTwo(indices, tempFrom, goodFrom, goodMidR, goodMidR, goodUntil);
            return splitMerge.mergeTwo(indices, tempFrom, weakFrom, newWeakMidL, weakMidL, newWeakUntil);
        }
    }

    private RecursiveTask<Integer> helperBAsync(final int goodFrom, final int goodUntil,
                                                final int weakFrom, final int weakUntil,
                                                final int obj,
                                                final int tempFrom, final int tempUntil) {
        return new RecursiveTask<Integer>() {
            @Override
            protected Integer compute() {
                return helperB(goodFrom, goodUntil, weakFrom, weakUntil, obj, tempFrom, tempUntil);
            }
        };
    }

    private int helperB(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom, int tempUntil) {
        int goodN = goodUntil - goodFrom;
        int weakN = weakUntil - weakFrom;
        if (goodN > 0 && weakN > 0) {
            if (goodN == 1) {
                return helperBGood1(goodFrom, weakFrom, weakUntil, obj);
            } else if (weakN == 1) {
                return helperBWeak1(goodFrom, goodUntil, weakFrom, obj);
            } else if (obj == 1) {
                return sweepB(goodFrom, goodUntil, weakFrom, weakUntil, tempFrom, tempUntil);
            } else if (helperBHookCondition(goodFrom, goodUntil, weakFrom, weakUntil, obj)) {
                return helperBHook(goodFrom, goodUntil, weakFrom, weakUntil, obj, tempFrom, tempUntil);
            } else {
                return helperBMain(goodFrom, goodUntil, weakFrom, weakUntil, obj, tempFrom, tempUntil);
            }
        } else {
            return weakUntil;
        }
    }

    private void twoDimensionalCase(double[][] points, int[] ranks) {
        // Also uses internalIndices and lastFrontOrdinates
        int maxRank = 1;
        int n = ranks.length;

        int lastII = internalIndices[0];
        double lastX = points[lastII][0];
        double lastY = points[lastII][1];
        double minY = lastY;

        // Point 0 always has rank 0.
        lastFrontOrdinates[0] = lastY;

        for (int i = 1; i < n; ++i) {
            int ii = internalIndices[i];
            double cx = points[ii][0];
            double cy = points[ii][1];

            if (cx == lastX && cy == lastY) {
                // Same point as the previous one.
                // The rank is the same as well.
                ranks[ii] = ranks[lastII];
            } else if (cy < minY) {
                // Y smaller than the smallest Y previously seen.
                // The rank is thus zero.
                minY = cy;
            } else {
                // At least the Y-smallest point dominates our point.
                int left, right;
                if (cy < lastY) {
                    // We are better than the previous point in Y.
                    // This means that we are at least that good.
                    left = 0;
                    right = ranks[lastII];
                } else {
                    // We are worse (or equal) than the previous point in Y.
                    // This means that we are worse than this point.
                    left = ranks[lastII];
                    right = maxRank;
                }
                // Running the binary search.
                while (right - left > 1) {
                    int mid = (left + right) >>> 1;
                    double midY = lastFrontOrdinates[mid];
                    if (cy < midY) {
                        right = mid;
                    } else {
                        left = mid;
                    }
                }
                // "right" is now our rank.
                ranks[ii] = right;
                lastFrontOrdinates[right] = cy;
                if (right == maxRank && maxRank <= maximalMeaningfulRank) {
                    ++maxRank;
                }
            }

            lastII = ii;
            lastX = cx;
            lastY = cy;
        }
    }

    String getThreadDescription() {
        return allowedThreads == -1 ? "unlimited threads" : allowedThreads + " thread(s)";
    }
}
