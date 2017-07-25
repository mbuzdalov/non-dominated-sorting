package ru.ifmo;

/**
 * This is the interface for factories producing instances of non-dominated sorting algorithms.
 * @author Maxim Buzdalov
 */
public interface NonDominatedSortingFactory {
    /**
     * Creates a new instance of a non-dominated sorting algorithm.
     * @param maximumPoints the maximum number of points to handle.
     * @param maximumDimension the maximum number of dimensions to handle.
     * @return the instance of the non-dominated sorting algorithm.
     */
    NonDominatedSorting getInstance(int maximumPoints, int maximumDimension);
}
