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

    // Data which is interval-shared between threads.
    private double[] temporary; // also used in 2D-only sweep
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

        temporary = new double[maximumPoints];
        ranks = new int[maximumPoints];

        if (maximumDimension > 2) {
            points = new double[maximumPoints][];
            transposedPoints = new double[maximumDimension][maximumPoints];
            splitMerge = new SplitMergeHelper(maximumPoints);
            hybrid = hybridWrapper.create(ranks, indices, points, transposedPoints);
        }
    }

    @Override
    protected void closeImpl() {
        temporary = null;
        ranks = null;
        points = null;
        transposedPoints = null;
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
        ArrayHelper.fillIdentity(indices, n);
        sorter.lexicographicalSort(points, indices, 0, n, dim);

        this.maximalMeaningfulRank = maximalMeaningfulRank;

        if (dim == 2) {
            // 2: Special case: binary search.
            twoDimensionalCase(points, ranks);
        } else {
            // 3: General case.
            // 3.1: Moving points in a sorted order to internal structures
            final int newN = ArraySorter.retainUniquePoints(points, indices, this.points, ranks);
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

    public static int kickOutOverflowedRanks(int[] indices, int[] ranks, int maximalMeaningfulRank, int from, int until) {
        int newUntil = from;
        for (int i = from; i < until; ++i) {
            int ii = indices[i];
            if (ranks[ii] <= maximalMeaningfulRank) {
                indices[newUntil] = ii;
                ++newUntil;
            }
        }
        return newUntil;
    }

    protected void postTransposePointHook(int newN) {}

    protected abstract int sweepA(int from, int until);
    protected abstract int sweepB(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int tempFrom);

    private int helperA(int from, int until, int obj) {
        int n = until - from;
        if (n <= 2) {
            if (n == 2) {
                int goodIndex = indices[from];
                int weakIndex = indices[from + 1];
                int goodRank = ranks[goodIndex];
                if (ranks[weakIndex] <= goodRank && DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(points[goodIndex], points[weakIndex], obj)) {
                    ranks[weakIndex] = 1 + goodRank;
                    if (goodRank >= maximalMeaningfulRank) {
                        return from + 1;
                    }
                }
            }
            return until;
        } else {
            while (obj > 1) {
                if (hybrid.helperAHookCondition(until - from, obj)) {
                    return hybrid.helperAHook(from, until, obj, maximalMeaningfulRank);
                } else if (ArrayHelper.transplantAndCheckIfSame(transposedPoints[obj], indices, from, until, temporary, from)) {
                    --obj;
                } else {
                    double median = ArrayHelper.destructiveMedian(temporary, from, until);
                    long split = splitMerge.splitInThree(transposedPoints[obj], indices, from, from, until, median);
                    int startMid = SplitMergeHelper.extractMid(split);
                    int startRight = SplitMergeHelper.extractRight(split);

                    int newStartMid = helperA(from, startMid, obj);
                    --obj;
                    int newStartRight = helperB(from, newStartMid, startMid, startRight, obj, from);
                    newStartRight = helperA(startMid, newStartRight, obj);
                    int newUntil = helperB(from, newStartMid, startRight, until, obj, from);
                    newUntil = helperB(startMid, newStartRight, startRight, newUntil, obj, from);
                    ++obj;
                    newUntil = helperA(startRight, newUntil, obj);

                    return splitMerge.mergeThree(indices, from, from, newStartMid, startMid, newStartRight, startRight, newUntil);
                }
            }
            return sweepA(from, until);
        }
    }

    public static int updateByPoint(int[] ranks, int[] indices, double[][] points, int maximalMeaningfulRank,
                                    int pointIndex, int from, int until, int obj) {
        int ri = ranks[pointIndex];
        if (ri == maximalMeaningfulRank) {
            return updateByPointCritical(ranks, indices, points, maximalMeaningfulRank, pointIndex, from, until, obj);
        } else {
            updateByPointNormal(ranks, indices, points, pointIndex, ri, from, until, obj);
            return until;
        }
    }

    private static void updateByPointNormal(int[] ranks, int[] indices, double[][] points, int pointIndex,
                                            int pointRank, int from, int until, int obj) {
        double[] pt = points[pointIndex];
        for (int i = from; i < until; ++i) {
            int ii = indices[i];
            if (ranks[ii] <= pointRank && DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(pt, points[ii], obj)) {
                ranks[ii] = pointRank + 1;
            }
        }
    }

    private static int updateByPointCritical(int[] ranks, int[] indices, double[][] points, int maximalMeaningfulRank,
                                             int pointIndex, int from, int until, int obj) {
        int minOverflow = until;
        double[] pt = points[pointIndex];
        for (int i = from; i < until; ++i) {
            int ii = indices[i];
            if (DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(pt, points[ii], obj)) {
                ranks[ii] = maximalMeaningfulRank + 1;
                if (minOverflow > i) {
                    minOverflow = i;
                }
            }
        }
        return kickOutOverflowedRanks(indices, ranks, maximalMeaningfulRank, minOverflow, until);
    }

    private int helperBWeak1Generic(int weak, int wi, int obj, int rw, int rw0, double[] wp, int goodMin, int goodMax) {
        for (int i = goodMax; i >= goodMin; --i) {
            int gi = indices[i];
            int gr = ranks[gi];
            if (rw <= gr && DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(points[gi], wp, obj)) {
                rw = gr + 1;
                if (rw > maximalMeaningfulRank) {
                    ranks[wi] = rw;
                    return weak;
                }
            }
        }
        if (rw != rw0) {
            ranks[wi] = rw;
        }
        return weak + 1;
    }

    private int helperBWeak1Rank0(int weak, int wi, int obj, double[] wp, int goodMin, int goodMax) {
        for (int i = goodMax; i >= goodMin; --i) {
            int gi = indices[i];
            if (DominanceHelper.strictlyDominatesAssumingLexicographicallySmaller(points[gi], wp, obj)) {
                int newRank = ranks[gi] + 1;
                if (newRank > maximalMeaningfulRank) {
                    ranks[wi] = newRank;
                    return weak;
                }
                return helperBWeak1Generic(weak, wi, obj, newRank, 0, wp, goodMin, i - 1);
            }
        }
        return weak + 1;
    }

    private int helperBWeak1(int goodFrom, int goodUntil, int weak, int obj) {
        int wi = indices[weak];
        int rw = ranks[wi];
        double[] wp = points[wi];
        if (rw == 0) {
            return helperBWeak1Rank0(weak, wi, obj, wp, goodFrom, goodUntil - 1);
        } else {
            return helperBWeak1Generic(weak, wi, obj, rw, rw, wp, goodFrom, goodUntil - 1);
        }
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
            goodUntil = ArrayHelper.findLastWhereNotGreater(indices, goodFrom, goodUntil, indices[weakUntil - 1]);
            weakFrom = ArrayHelper.findWhereNotSmaller(indices, weakFrom, weakUntil, indices[goodFrom]);
        }
        int goodN = goodUntil - goodFrom;
        int weakN = weakUntil - weakFrom;
        if (goodN > 0 && weakN > 0) {
            if (goodN == 1) {
                return updateByPoint(ranks, indices, points, maximalMeaningfulRank, indices[goodFrom], weakFrom, weakUntil, obj);
            } else if (weakN == 1) {
                return helperBWeak1(goodFrom, goodUntil, weakFrom, obj);
            } else {
                while (obj > 1) {
                    if (hybrid.helperBHookCondition(goodFrom, goodUntil, weakFrom, weakUntil, obj)) {
                        return hybrid.helperBHook(goodFrom, goodUntil, weakFrom, weakUntil, obj, tempFrom, maximalMeaningfulRank);
                    } else {
                        double[] currentPoints = transposedPoints[obj];
                        switch (ArrayHelper.transplantAndDecide(currentPoints, indices,
                                goodFrom, goodUntil, weakFrom, weakUntil, temporary, tempFrom)) {
                            case ArrayHelper.TRANSPLANT_LEFT_NOT_GREATER:
                                --obj;
                                break;
                            case ArrayHelper.TRANSPLANT_RIGHT_SMALLER:
                                return weakUntil;
                            case ArrayHelper.TRANSPLANT_GENERAL_CASE:
                                double median = ArrayHelper.destructiveMedian(temporary, tempFrom, tempFrom + goodUntil - goodFrom + weakUntil - weakFrom);
                                long goodSplit = splitMerge.splitInThree(currentPoints, indices, tempFrom, goodFrom, goodUntil, median);
                                int goodMidL = SplitMergeHelper.extractMid(goodSplit);
                                int goodMidR = SplitMergeHelper.extractRight(goodSplit);
                                long weakSplit = splitMerge.splitInThree(currentPoints, indices, tempFrom, weakFrom, weakUntil, median);
                                int weakMidL = SplitMergeHelper.extractMid(weakSplit);
                                int weakMidR = SplitMergeHelper.extractRight(weakSplit);
                                int tempMid = tempFrom + ((goodUntil - goodFrom + weakUntil - weakFrom) >>> 1);

                                --obj;
                                int newWeakUntil = helperB(goodFrom, goodMidL, weakMidR, weakUntil, obj, tempFrom);
                                newWeakUntil = helperB(goodMidL, goodMidR, weakMidR, newWeakUntil, obj, tempFrom);

                                int newWeakMidR = helperB(goodFrom, goodMidL, weakMidL, weakMidR, obj, tempFrom);
                                newWeakMidR = helperB(goodMidL, goodMidR, weakMidL, newWeakMidR, obj, tempFrom);
                                ++obj;

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
                    }
                }
                return sweepB(goodFrom, goodUntil, weakFrom, weakUntil, tempFrom);
            }
        }
        return weakUntil;
    }

    private void twoDimensionalCase(double[][] points, int[] ranks) {
        int maxRank = 1;
        int n = ranks.length;

        double[] firstPoint = points[indices[0]];
        double lastX = firstPoint[0];
        double lastY = firstPoint[1];
        int lastRank = 0;

        // This is used here instead of temporary[0] to make it slightly faster.
        double minY = lastY;

        for (int i = 1; i < n; ++i) {
            int ii = indices[i];
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
                    double midY = temporary[mid];
                    if (currY < midY) {
                        right = mid;
                    } else {
                        left = mid;
                    }
                }
                // "right" is now our rank.
                ranks[ii] = lastRank = right;
                temporary[right] = currY;
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
}
