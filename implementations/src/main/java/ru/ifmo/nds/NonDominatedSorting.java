package ru.ifmo.nds;

import java.util.Arrays;
import java.util.Objects;

/**
 * This is the base for classes which actually perform non-dominated sorting.
 *
 * @author Maxim Buzdalov
 */
public abstract class NonDominatedSorting implements AutoCloseable {
    private boolean closeWasCalled = false;
    private final int maximumPoints;
    private final int maximumDimension;

    protected NonDominatedSorting(int maximumPoints, int maximumDimension) {
        this.maximumPoints = maximumPoints;
        this.maximumDimension = maximumDimension;
    }

    /**
     * Returns the name of the algorithm.
     * @return the name of the algorithm.
     */
    public abstract String getName();

    /**
     * Returns the maximum number of points this sorter can handle.
     * @return the maximum number of points this sorter can handle.
     */
    public final int getMaximumPoints() {
        return maximumPoints;
    }

    /**
     * Returns the maximum number of dimensions this sorter can handle.
     * @return the maximum number of dimensions this sorter can handle.
     */
    public final int getMaximumDimension() {
        return maximumDimension;
    }

    /**
     * Releases all resources taken by the non-dominated sorting algorithm.
     */
    public final void close() {
        if (closeWasCalled) {
            throw new IllegalStateException("close() has already been called");
        }
        closeWasCalled = true;
        try {
            closeImpl();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Performs non-dominated sorting.
     * @param points the array of points to be sorted.
     * @param ranks the array to be filled with ranks of points.
     */
    public final void sort(double[][] points, int[] ranks) {
        sort(points, ranks, ranks == null ? 0 : ranks.length);
    }

    /**
     * Performs non-dominated sorting. All ranks above the given {@code maximalMeaningfulRank} will be reported
     * as {@code maximalMeaningfulRank + 1}.
     *
     * @param points the array of points to be sorted.
     * @param ranks the array to be filled with ranks of points.
     * @param maximalMeaningfulRank the maximal rank which is meaningful to the caller.
     *                              All ranks above will be reported as {@code maximalMeaningfulRank + 1}.
     *                              The safe value to get all ranks correct is {@code points.length}.
     */
    public final void sort(double[][] points, int[] ranks, int maximalMeaningfulRank) {
        requirePointsAreNonNull(points);
        Objects.requireNonNull(ranks, "The array of ranks must not be null");

        checkNumbersOfPoints(points.length, ranks.length);

        if (points.length == 0) {
            // Nothing to be done here.
            return;
        }

        if (maximalMeaningfulRank < 0) {
            throw new IllegalArgumentException("Maximal meaningful rank must be non-negative");
        }

        int dimension = checkAndGetDimension(points);
        if (dimension == 0) {
            Arrays.fill(ranks, 0);
        } else {
            sortChecked(points, ranks, maximalMeaningfulRank);
            filterMaximumMeaningfulRank(ranks, maximalMeaningfulRank);
        }
    }

    /**
     * Performs actual release of any resources hold by the algorithm.
     * @throws Exception if something nasty happens.
     */
    protected abstract void closeImpl() throws Exception;

    /**
     * Performs actual sorting. Assumes the input arrays are valid.
     * @param points the points to be sorted.
     * @param ranks the array of ranks to be filled.
     * @param maximalMeaningfulRank the maximal rank which is meaningful to the caller.
     *                              All ranks above can be treated as same.
     */
    protected abstract void sortChecked(double[][] points, int[] ranks, int maximalMeaningfulRank);

    private void requirePointsAreNonNull(double[][] points) {
        Objects.requireNonNull(points, "The array of points must not be null");
        for (double[] point : points) {
            Objects.requireNonNull(point, "The points to be sorted must not be null");
            for (double coordinate : point) {
                if (Double.isNaN(coordinate) || Double.isInfinite(coordinate)) {
                    throw new IllegalArgumentException("Coordinates of points to be sorted must not be NaN or Inf");
                }
            }
        }
    }

    private void filterMaximumMeaningfulRank(int[] ranks, int maximalMeaningfulRank) {
        for (int i = 0; i < ranks.length; ++i) {
            if (ranks[i] > maximalMeaningfulRank) {
                ranks[i] = maximalMeaningfulRank + 1;
            }
        }
    }

    private int checkAndGetDimension(double[][] points) {
        int dimension = points[0].length;
        for (int i = 1; i < points.length; ++i) {
            if (points[i].length != dimension) {
                throw new IllegalArgumentException("All points to be sorted must have equal dimension");
            }
        }
        if (dimension > this.maximumDimension) {
            throw new IllegalArgumentException(
                    "The dimension of points to be sorted, " + dimension
                            + ", must not exceed the maximum dimension, " + this.maximumDimension
                            + ", which this instance of NonDominatedSorting can handle");
        }
        return dimension;
    }

    private void checkNumbersOfPoints(int pointsLength, int ranksLength) {
        if (pointsLength > this.maximumPoints) {
            throw new IllegalArgumentException(
                    "The number of points to be sorted, " + pointsLength
                            + ", must not exceed the maximum number of points, " + this.maximumPoints
                            + ", which this instance of NonDominatedSorting can handle");
        }
        if (pointsLength != ranksLength) {
            throw new IllegalArgumentException(
                    "The number of points, " + pointsLength
                            + ", must coincide with the length of the array for ranks, which is " + ranksLength);
        }
    }
}
