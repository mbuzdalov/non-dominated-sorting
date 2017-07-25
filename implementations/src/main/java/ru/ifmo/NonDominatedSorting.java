package ru.ifmo;

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
     *
     * @param points the array of points to be sorted.
     * @param ranks the array to be filled with ranks of points.
     */
    public final void sort(double[][] points, int[] ranks) {
        Objects.requireNonNull(points, "The array of points must not be null");
        Objects.requireNonNull(ranks, "The array of ranks must not be null");

        int myMaximumPoints = getMaximumPoints();
        int myMaximumDimension = getMaximumDimension();

        if (points.length > myMaximumPoints) {
            throw new IllegalArgumentException(
                    "The number of points to be sorted, " + points.length
                            + ", must not exceed the maximum number of points, " + myMaximumPoints
                            + ", which this instance of NonDominatedSorting can handle");
        }
        if (points.length != ranks.length) {
            throw new IllegalArgumentException(
                    "The number of points, " + points.length
                            + ", must coincide with the length of the array for ranks, which is " + ranks.length);
        }
        if (points.length == 0) {
            // Nothing to be done here.
            return;
        }
        for (double[] point : points) {
            Objects.requireNonNull(point, "The points to be sorted must not be null");
            for (double coordinate : point) {
                if (Double.isNaN(coordinate) || Double.isInfinite(coordinate)) {
                    throw new IllegalArgumentException("Coordinates of points to be sorted must not be NaN or Inf");
                }
            }
        }
        int dimension = points[0].length;
        if (dimension > myMaximumDimension) {
            throw new IllegalArgumentException(
                    "The dimension of points to be sorted, " + dimension
                            + ", must not exceed the maximum dimension, " + myMaximumDimension
                            + ", which this instance of NonDominatedSorting can handle");
        }
        for (int i = 1; i < points.length; ++i) {
            if (points[i].length != dimension) {
                throw new IllegalArgumentException("All points to be sorted must have equal dimension");
            }
        }
        sortChecked(points, ranks);
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
     */
    protected abstract void sortChecked(double[][] points, int[] ranks);
}
