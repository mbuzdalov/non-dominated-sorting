package ru.ifmo.nds.jfb;

import ru.ifmo.nds.NonDominatedSorting;
import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.DoubleArraySorter;
import ru.ifmo.nds.util.MedianFinder;
import ru.ifmo.nds.util.RankQueryStructure;

import java.util.Arrays;

public abstract class AbstractJFBSorting extends NonDominatedSorting {
    // Pre-allocated
    int[] indices;
    int[] ranks;
    int maximalMeaningfulRank;

    private DoubleArraySorter sorter;
    private MedianFinder medianFinder;
    private RankQueryStructure rankQuery;
    private int[] internalIndices;
    private double[] lastFrontOrdinates;
    private int[] splitScratchL, splitScratchM, splitScratchR;
    private double[][] transposedPoints;

    private int[] overflowedIndices;
    private int overflowedCount;

    // Various answers
    private int splitL;
    private int splitM;

    // Partially carrying input data
    private double[][] points;

    AbstractJFBSorting(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);

        sorter = new DoubleArraySorter(maximumPoints);
        medianFinder = new MedianFinder(maximumPoints);
        indices = new int[maximumPoints];
        ranks = new int[maximumPoints];
        points = new double[maximumPoints][];
        transposedPoints = new double[maximumDimension][maximumPoints];
        rankQuery = createStructure(maximumPoints);

        internalIndices = new int[maximumPoints];
        lastFrontOrdinates = new double[maximumPoints];
        splitScratchL = new int[maximumPoints];
        splitScratchM = new int[maximumPoints];
        splitScratchR = new int[maximumPoints];

        overflowedIndices = new int[maximumPoints];
    }

    @Override
    protected void closeImpl() throws Exception {
        sorter = null;
        medianFinder = null;
        indices = null;
        ranks = null;
        points = null;
        rankQuery = null;
        transposedPoints = null;

        internalIndices = null;
        lastFrontOrdinates = null;
        splitScratchL = null;
        splitScratchM = null;
        splitScratchR = null;
        overflowedIndices = null;
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

            overflowedCount = 0;

            // 3.3: Calling the actual sorting
            int nonOverflowed = helperA(0, newN, dim - 1);
            if (nonOverflowed + overflowedCount != newN) {
                throw new AssertionError("nonOverflowed = " + nonOverflowed
                        + " overflowed = " + overflowedCount
                        + " newN = " + newN);
            }

            // 3.4: Applying the results back. After that, the argument "ranks" array stops being abused.
            for (int i = 0; i < n; ++i) {
                ranks[i] = this.ranks[ranks[i]];
                this.points[i] = null;
            }
        }
    }

    void reportOverflowedRank(int index) {
        ranks[index] = maximalMeaningfulRank + 1;
        overflowedIndices[overflowedCount++] = index;
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
        if (ranks[weakIndex] <= ranks[goodIndex]) {
            ranks[weakIndex] = 1 + ranks[goodIndex];
            if (ranks[weakIndex] > maximalMeaningfulRank) {
                ranks[weakIndex] = maximalMeaningfulRank + 1;
                reportOverflowedRank(weakIndex);
                return false;
            }
        }
        return true;
    }

    private void splitInTwo(int from, int until, double median, int obj, boolean equalToLeft) {
        int left = 0, right = 0;
        double[] local = transposedPoints[obj];
        for (int i = from; i < until; ++i) {
            int ii = indices[i];
            double v = local[ii];
            if (v < median || (equalToLeft && v == median)) {
                splitScratchL[left++] = ii;
            } else {
                splitScratchR[right++] = ii;
            }
        }
        System.arraycopy(splitScratchL, 0, indices, from, left);
        System.arraycopy(splitScratchR, 0, indices, from + left, right);
        splitL = left;
    }

    private void splitInThree(int from, int until, double median, int obj) {
        int l = 0, m = 0, r = 0;
        double[] local = transposedPoints[obj];
        for (int i = from; i < until; ++i) {
            int ii = indices[i];
            double v = local[ii];
            if (v < median) {
                splitScratchL[l++] = ii;
            } else if (v == median) {
                splitScratchM[m++] = ii;
            } else {
                splitScratchR[r++] = ii;
            }
        }
        System.arraycopy(splitScratchL, 0, indices, from, l);
        System.arraycopy(splitScratchM, 0, indices, from + l, m);
        System.arraycopy(splitScratchR, 0, indices, from + l + m, r);
        splitL = l;
        splitM = m;
    }

    private int mergeTwo(int fromLeft, int untilLeft, int fromRight, int untilRight) {
        int target = 0;
        int l = fromLeft, r = fromRight;
        while (l < untilLeft && r < untilRight) {
            if (indices[l] <= indices[r]) {
                splitScratchM[target++] = indices[l++];
            } else {
                splitScratchM[target++] = indices[r++];
            }
        }
        System.arraycopy(indices, l, splitScratchM, target, untilLeft - l);
        target += untilLeft - l;
        System.arraycopy(indices, r, splitScratchM, target, untilRight - r);
        target += untilRight - r;
        System.arraycopy(splitScratchM, 0, indices, fromLeft, target);
        return fromLeft + target;
    }

    protected abstract RankQueryStructure createStructure(int maximumPoints);

    private int sweepA(int from, int until) {
        double[] local = transposedPoints[1];
        if (rankQuery.needsPossibleKeys()) {
            for (int i = from; i < until; ++i) {
                rankQuery.addPossibleKey(local[indices[i]]);
            }
        }
        rankQuery.init();
        int newUntil = from;
        for (int i = from; i < until; ++i) {
            int curr = indices[i];
            double currY = local[curr];
            int result = Math.max(ranks[curr], rankQuery.getMaximumWithKeyAtMost(currY) + 1);
            ranks[curr] = result;
            if (result > maximalMeaningfulRank) {
                reportOverflowedRank(curr);
            } else {
                indices[newUntil++] = curr;
                rankQuery.put(currY, result);
            }
        }
        rankQuery.clear();
        return newUntil;
    }

    private int sweepB(int goodFrom, int goodUntil, int weakFrom, int weakUntil) {
        double[] local = transposedPoints[1];
        if (rankQuery.needsPossibleKeys()) {
            for (int i = goodFrom; i < goodUntil; ++i) {
                rankQuery.addPossibleKey(local[indices[i]]);
            }
        }
        rankQuery.init();
        int goodI = goodFrom;
        int newWeakUntil = weakFrom;
        for (int weakI = weakFrom; weakI < weakUntil; ++weakI) {
            int weakCurr = indices[weakI];
            while (goodI < goodUntil && indices[goodI] < weakCurr) {
                int goodCurr = indices[goodI++];
                rankQuery.put(local[goodCurr], ranks[goodCurr]);
            }
            ranks[weakCurr] = Math.max(ranks[weakCurr], rankQuery.getMaximumWithKeyAtMost(local[weakCurr]) + 1);
            if (ranks[weakCurr] > maximalMeaningfulRank) {
                reportOverflowedRank(weakCurr);
            } else {
                indices[newWeakUntil++] = weakCurr;
            }
        }
        rankQuery.clear();
        return newWeakUntil;
    }

    protected boolean helperAHookCondition(int size, int obj) {
        return false;
    }
    protected int helperAHook(int from, int until, int obj) {
        throw new UnsupportedOperationException("helperAHook not yet implemented");
    }

    private int helperA(int from, int until, int obj) {
        int n = until - from;
        if (n <= 2) {
            if (n == 2) {
                int goodIndex = indices[from];
                int weakIndex = indices[from + 1];
                if (strictlyDominatesAssumingNotSame(goodIndex, weakIndex, obj)) {
                    if (!tryUpdateRank(goodIndex, weakIndex)) {
                        return from + 1;
                    }
                }
            }
            return until;
        } else if (obj == 1) {
            return sweepA(from, until);
        } else if (helperAHookCondition(until - from, obj)) {
            return helperAHook(from, until, obj);
        } else {
            medianFinder.resetMedian();
            medianFinder.consumeDataForMedian(transposedPoints[obj], indices, from, until);
            if (medianFinder.getLastMedianConsumptionMin() == medianFinder.getLastMedianConsumptionMax()) {
                return helperA(from, until, obj - 1);
            } else {
                double median = medianFinder.findMedian();
                int smallerThanMedian = medianFinder.howManySmallerThanMedian();
                int largerThanMedian = medianFinder.howManyLargerThanMedian();
                int equalToMedian = n - smallerThanMedian - largerThanMedian;
                if (equalToMedian < n / 2) {
                    // Few enough median-valued points, use two-way splitting.
                    splitInTwo(from, until, median, obj, smallerThanMedian < largerThanMedian);
                    int middle = from + splitL;
                    int newMiddle = helperA(from, middle, obj);
                    int newUntil = helperB(from, newMiddle, middle, until, obj - 1);
                    newUntil = helperA(middle, newUntil, obj);
                    return mergeTwo(from, newMiddle, middle, newUntil);
                } else {
                    // Too many median-valued points, use three-way splitting.
                    splitInThree(from, until, median, obj);
                    int startMid = from + splitL;
                    int startRight = startMid + splitM;
                    int newStartMid = helperA(from, startMid, obj);
                    int newStartRight = helperB(from, newStartMid, startMid, startRight, obj - 1);
                    newStartRight = helperA(startMid, newStartRight, obj - 1);
                    newStartRight = mergeTwo(from, newStartMid, startMid, newStartRight);
                    int newUntil = helperB(from, newStartRight, startRight, until, obj - 1);
                    newUntil = helperA(startRight, newUntil, obj);
                    return mergeTwo(from, newStartRight, startRight, newUntil);
                }
            }
        }
    }

    private int helperBGood1(int good, int weakFrom, int weakUntil, int obj) {
        int gi = indices[good];
        // Binary search to discard points which are definitely not dominated.
        int bs = Arrays.binarySearch(indices, weakFrom, weakUntil, gi);
        int weakStart = -bs - 1;
        int targetIndex = weakStart;
        for (int i = weakStart; i < weakUntil; ++i) {
            int wi = indices[i];
            if (strictlyDominatesAssumingNotSame(gi, wi, obj)) {
                if (!tryUpdateRank(gi, wi)) {
                    continue;
                }
            }
            indices[targetIndex++] = wi;
        }
        return targetIndex;
    }

    private int helperBWeak1(int goodFrom, int goodUntil, int weak, int obj) {
        int wi = indices[weak];
        // Binary search to discard points which definitely do not dominate.
        int bs = Arrays.binarySearch(indices, goodFrom, goodUntil, wi);
        int goodFinish = -bs - 1;
        for (int i = goodFrom; i < goodFinish; ++i) {
            int gi = indices[i];
            if (strictlyDominatesAssumingNotSame(gi, wi, obj)) {
                if (!tryUpdateRank(gi, wi)) {
                    return weak;
                }
            }
        }
        return weak + 1;
    }

    protected boolean helperBHookCondition(int goodSize, int weakSize, int obj) {
        return false;
    }
    protected int helperBHook(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        throw new UnsupportedOperationException("helperBHook not yet implemented");
    }

    private int helperB(int goodFrom, int goodUntil, int weakFrom, int weakUntil, int obj) {
        int goodN = goodUntil - goodFrom;
        int weakN = weakUntil - weakFrom;
        if (goodN > 0 && weakN > 0) {
            if (goodN == 1) {
                return helperBGood1(goodFrom, weakFrom, weakUntil, obj);
            } else if (weakN == 1) {
                return helperBWeak1(goodFrom, goodUntil, weakFrom, obj);
            } else if (obj == 1) {
                return sweepB(goodFrom, goodUntil, weakFrom, weakUntil);
            } else if (helperBHookCondition(goodUntil - goodFrom, weakUntil - weakFrom, obj)) {
                return helperBHook(goodFrom, goodUntil, weakFrom, weakUntil, obj);
            } else {
                medianFinder.resetMedian();
                medianFinder.consumeDataForMedian(transposedPoints[obj], indices, goodFrom, goodUntil);
                double goodMaxObj = medianFinder.getLastMedianConsumptionMax();
                medianFinder.consumeDataForMedian(transposedPoints[obj], indices, weakFrom, weakUntil);
                double weakMinObj = medianFinder.getLastMedianConsumptionMin();
                if (goodMaxObj <= weakMinObj) {
                    return helperB(goodFrom, goodUntil, weakFrom, weakUntil, obj - 1);
                } else {
                    double median = medianFinder.findMedian();
                    int totalSmallerThanMedian = medianFinder.howManySmallerThanMedian();
                    int totalLargerThanMedian = medianFinder.howManyLargerThanMedian();
                    int totalEqualToMedian = (goodN + weakN) - totalLargerThanMedian - totalSmallerThanMedian;
                    if (totalEqualToMedian < (goodN + weakN) / 2) {
                        // Few enough median-valued points, use two-way splitting.
                        boolean leanLeft = totalSmallerThanMedian < totalLargerThanMedian;
                        splitInTwo(goodFrom, goodUntil, median, obj, leanLeft);
                        int goodMid = goodFrom + splitL;
                        splitInTwo(weakFrom, weakUntil, median, obj, leanLeft);
                        int weakMid = weakFrom + splitL;

                        int newWeakMid = helperB(goodFrom, goodMid, weakFrom, weakMid, obj);
                        int newWeakUntil = helperB(goodFrom, goodMid, weakMid, weakUntil, obj - 1);
                        newWeakUntil = helperB(goodMid, goodUntil, weakMid, newWeakUntil, obj);
                        mergeTwo(goodFrom, goodMid, goodMid, goodUntil);
                        return mergeTwo(weakFrom, newWeakMid, weakMid, newWeakUntil);
                    } else {
                        // Too many median-valued points, use three-way splitting.
                        splitInThree(goodFrom, goodUntil, median, obj);
                        int goodMidL = goodFrom + splitL;
                        int goodMidR = goodMidL + splitM;
                        splitInThree(weakFrom, weakUntil, median, obj);
                        int weakMidL = weakFrom + splitL;
                        int weakMidR = weakMidL + splitM;

                        int newWeakMidL = helperB(goodFrom, goodMidL, weakFrom, weakMidL, obj);
                        int newWeakUntil = helperB(goodMidR, goodUntil, weakMidR, weakUntil, obj);
                        mergeTwo(goodFrom, goodMidL, goodMidL, goodMidR);
                        newWeakUntil = mergeTwo(weakMidL, weakMidR, weakMidR, newWeakUntil);
                        newWeakUntil = helperB(goodFrom, goodMidR, weakMidL, newWeakUntil, obj - 1);
                        mergeTwo(goodFrom, goodMidR, goodMidR, goodUntil);
                        return mergeTwo(weakFrom, newWeakMidL, weakMidL, newWeakUntil);
                    }
                }
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
}
