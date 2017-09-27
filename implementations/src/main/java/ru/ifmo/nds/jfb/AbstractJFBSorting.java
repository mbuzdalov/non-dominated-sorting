package ru.ifmo.nds.jfb;

import java.util.Arrays;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.*;

public abstract class AbstractJFBSorting extends NonDominatedSorting {
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
    private int[] splitScratchM, splitScratchR;

    AbstractJFBSorting(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);

        sorter = new DoubleArraySorter(maximumPoints);
        medianSwap = new double[maximumPoints];
        indices = new int[maximumPoints];
        ranks = new int[maximumPoints];
        points = new double[maximumPoints][];
        transposedPoints = new double[maximumDimension][maximumPoints];
        rankQuery = createStructure(maximumPoints);

        internalIndices = new int[maximumPoints];
        lastFrontOrdinates = new double[maximumPoints];
        splitScratchM = new int[maximumPoints];
        splitScratchR = new int[maximumPoints];
    }

    @Override
    protected void closeImpl() throws Exception {
        sorter = null;
        medianSwap = null;
        indices = null;
        ranks = null;
        points = null;
        rankQuery = null;
        transposedPoints = null;

        internalIndices = null;
        lastFrontOrdinates = null;
        splitScratchM = null;
        splitScratchR = null;
    }

    @Override
    protected final void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        int n = points.length;
        int dim = points[0].length;
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
            int newN = DoubleArraySorter.retainUniquePoints(points, internalIndices, this.points, ranks);
            Arrays.fill(this.ranks, 0, newN, 0);
            ArrayHelper.fillIdentity(this.indices, newN);

            // 3.2: Transposing points. This should fit in cache for reasonable dimensions.
            for (int i = 0; i < newN; ++i) {
                for (int j = 0; j < dim; ++j) {
                    transposedPoints[j][i] = this.points[i][j];
                }
            }

            // 3.3: Calling the actual sorting
            helperA(0, newN, dim - 1);

            // 3.4: Applying the results back. After that, the argument "ranks" array stops being abused.
            for (int i = 0; i < n; ++i) {
                ranks[i] = this.ranks[ranks[i]];
                this.points[i] = null;
            }
        }
    }

    int kickOutOverflowedRanks(int from, int until) {
        int newUntil = from;
        for (int i = from; i < until; ++i) {
            int ii = indices[i];
            if (ranks[ii] <= maximalMeaningfulRank) {
                indices[newUntil++] = ii;
            }
        }
        return newUntil;
    }

    boolean strictlyDominatesAssumingNotSame(int goodIndex, int weakIndex, int maxObj) {
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
            ranks[weakIndex] = 1 + ranks[goodIndex];
            return rg < maximalMeaningfulRank;
        }
        return true;
    }

    private int splitInTwo(int tempFrom, int from, int until, double median, int obj, boolean equalToLeft, double minVal, double maxVal) {
        if (minVal == median && maxVal == median) {
            return equalToLeft ? until : from;
        } else if (minVal > median || !equalToLeft && minVal == median) {
            return from;
        } else if (maxVal < median || equalToLeft && maxVal == median) {
            return until;
        } else {
            int left = from, right = tempFrom;
            double[] local = transposedPoints[obj];
            for (int i = from; i < until; ++i) {
                int ii = indices[i];
                double v = local[ii];
                if (v < median || (equalToLeft && v == median)) {
                    indices[left++] = ii;
                } else {
                    splitScratchR[right++] = ii;
                }
            }
            System.arraycopy(splitScratchR, tempFrom, indices, left, right - tempFrom);
            return left;
        }
    }

    private SplitThreeResult splitInThree(int tempFrom, int from, int until, double median, int obj, double minVal, double maxVal) {
        if (minVal == median && maxVal == median) {
            return new SplitThreeResult(from, until);
        } else if (minVal > median) {
            return new SplitThreeResult(from, from);
        } else if (maxVal < median) {
            return new SplitThreeResult(until, until);
        } else {
            int l = from, m = tempFrom, r = tempFrom;
            double[] local = transposedPoints[obj];
            for (int i = from; i < until; ++i) {
                int ii = indices[i];
                double v = local[ii];
                if (v < median) {
                    indices[l++] = ii;
                } else if (v == median) {
                    splitScratchM[m++] = ii;
                } else {
                    splitScratchR[r++] = ii;
                }
            }
            System.arraycopy(splitScratchM, tempFrom, indices, l, m - tempFrom);
            System.arraycopy(splitScratchR, tempFrom, indices, l + m - tempFrom, r - tempFrom);
            return new SplitThreeResult(l, m - tempFrom + l);
        }
    }

    private int mergeTwo(int tempFrom, int fromLeft, int untilLeft, int fromRight, int untilRight) {
        int target = tempFrom;
        int l = fromLeft, r = fromRight;
        while (l < untilLeft && r < untilRight) {
            if (indices[l] <= indices[r]) {
                splitScratchM[target++] = indices[l++];
            } else {
                splitScratchM[target++] = indices[r++];
            }
        }
        int newR = fromLeft + (target - tempFrom) + untilLeft - l;
        if (r != newR && untilRight > r) {
            // copy the remainder of right to its place
            System.arraycopy(indices, r, indices, newR, untilRight - r);
        }
        if (l != fromLeft + (target - tempFrom) && untilLeft > l) {
            // copy the remainder of left to its place
            System.arraycopy(indices, l, indices, fromLeft + (target - tempFrom), untilLeft - l);
        }
        if (target > tempFrom) {
            // copy the merged part
            System.arraycopy(splitScratchM, tempFrom, indices, fromLeft, target - tempFrom);
        }
        return fromLeft + (target - tempFrom) + untilLeft - l + untilRight - r;
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
        int n = until - from;
        ArrayHelper.transplant(transposedPoints[obj], indices, from, until, medianSwap, from);
        double objMin = ArrayHelper.min(medianSwap, from, until);
        double objMax = ArrayHelper.max(medianSwap, from, until);
        if (objMin == objMax) {
            return helperA(from, until, obj - 1);
        } else {
            double median = ArrayHelper.destructiveMedian(medianSwap, from, until);
            int smallerThanMedian = ArrayHelper.countSmaller(medianSwap, from, until, median);
            int largerThanMedian = ArrayHelper.countGreater(medianSwap, from, until, median);
            int equalToMedian = n - smallerThanMedian - largerThanMedian;
            if (equalToMedian < n / 2) {
                // Few enough median-valued points, use two-way splitting.
                int middle = splitInTwo(from, from, until, median, obj, smallerThanMedian < largerThanMedian, objMin, objMax);
                int newMiddle = helperA(from, middle, obj);
                int newUntil = helperB(from, newMiddle, middle, until, obj - 1, from, until);
                newUntil = helperA(middle, newUntil, obj);
                return mergeTwo(from, from, newMiddle, middle, newUntil);
            } else {
                // Too many median-valued points, use three-way splitting.
                SplitThreeResult split = splitInThree(from, from, until, median, obj, objMin, objMax);
                int startMid = split.startMid;
                int startRight = split.startRight;
                int newStartMid = helperA(from, startMid, obj);
                int newStartRight = helperB(from, newStartMid, startMid, startRight, obj - 1, from, until);
                newStartRight = helperA(startMid, newStartRight, obj - 1);
                newStartRight = mergeTwo(from, from, newStartMid, startMid, newStartRight);
                int newUntil = helperB(from, newStartRight, startRight, until, obj - 1, from, until);
                newUntil = helperA(startRight, newUntil, obj);
                return mergeTwo(from, from, newStartRight, startRight, newUntil);
            }
        }
    }

    // returns whether weak is meaningful after all checks
    private boolean ifDominatesUpdateRankAndCheckWhetherMeaningful(int good, int weak, int obj) {
        if (strictlyDominatesAssumingNotSame(good, weak, obj)) {
            if (!tryUpdateRank(good, weak)) {
                return false;
            }
        }
        return true;
    }

    private int helperA(int from, int until, int obj) {
        int n = until - from;
        if (n <= 2) {
            if (n == 2) {
                int goodIndex = indices[from];
                int weakIndex = indices[from + 1];
                if (!ifDominatesUpdateRankAndCheckWhetherMeaningful(goodIndex, weakIndex, obj)) {
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

    int updateByPoint(int pointIndex, int from, int until, int obj) {
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
            if (!ifDominatesUpdateRankAndCheckWhetherMeaningful(gi, wi, obj)) {
                return weak;
            }
        }
        return weak + 1;
    }

    protected boolean helperBHookCondition(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        return false;
    }
    protected int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        throw new UnsupportedOperationException("helperBHook not yet implemented");
    }

    private int helperBMain(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj, int tempFrom, int tempUntil) {
        if (tempUntil - tempFrom < goodUntil - goodFrom + weakUntil - weakFrom) {
            throw new AssertionError();
        }

        int goodN = goodUntil - goodFrom;
        int weakN = weakUntil - weakFrom;
        if (goodN == 0 || weakN == 0) {
            return weakUntil;
        }

        int medianGood = ArrayHelper.transplant(transposedPoints[obj], indices, goodFrom, goodUntil, medianSwap, tempFrom);
        double goodMaxObj = ArrayHelper.max(medianSwap, tempFrom, medianGood);
        double goodMinObj = ArrayHelper.min(medianSwap, tempFrom, medianGood);
        int medianWeak = ArrayHelper.transplant(transposedPoints[obj], indices, weakFrom, weakUntil, medianSwap, medianGood);
        double weakMaxObj = ArrayHelper.max(medianSwap, medianGood, medianWeak);
        double weakMinObj = ArrayHelper.min(medianSwap, medianGood, medianWeak);
        if (goodMaxObj <= weakMinObj) {
            return helperB(goodFrom, goodUntil, weakFrom, weakUntil, obj - 1, tempFrom, tempUntil);
        } else {
            double median = ArrayHelper.destructiveMedian(medianSwap, tempFrom, medianWeak);
            SplitThreeResult goodSplit = splitInThree(tempFrom, goodFrom, goodUntil, median, obj, goodMinObj, goodMaxObj);
            int goodMidL = goodSplit.startMid;
            int goodMidR = goodSplit.startRight;
            SplitThreeResult weakSplit = splitInThree(tempFrom, weakFrom, weakUntil, median, obj, weakMinObj, weakMaxObj);
            int weakMidL = weakSplit.startMid;
            int weakMidR = weakSplit.startRight;
            int tempMid = (tempFrom + tempUntil) >>> 1;

            int newWeakMidL = helperB(goodFrom, goodMidL, weakFrom, weakMidL, obj, tempFrom, tempMid);
            int newWeakUntil = helperB(goodMidR, goodUntil, weakMidR, weakUntil, obj, tempMid, tempUntil);

            mergeTwo(tempFrom, goodFrom, goodMidL, goodMidL, goodMidR);
            newWeakUntil = mergeTwo(tempFrom, weakMidL, weakMidR, weakMidR, newWeakUntil);
            newWeakUntil = helperB(goodFrom, goodMidR, weakMidL, newWeakUntil, obj - 1, tempFrom, tempUntil);
            mergeTwo(tempFrom, goodFrom, goodMidR, goodMidR, goodUntil);
            return mergeTwo(tempFrom, weakFrom, newWeakMidL, weakMidL, newWeakUntil);
        }
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
                return helperBHook(goodFrom, goodUntil, weakFrom, weakUntil, obj);
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

    private static final class SplitThreeResult {
        final int startMid;
        final int startRight;

        private SplitThreeResult(int startMid, int startRight) {
            this.startMid = startMid;
            this.startRight = startRight;
        }
    }
}
