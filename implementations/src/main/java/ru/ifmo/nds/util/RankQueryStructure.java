package ru.ifmo.nds.util;

/**
 * A rank query structure to be used in 2D algorithms for non-dominated sorting (with lower bounds).
 *
 * @author Maxim Buzdalov
 */
public abstract class RankQueryStructure {
    /**
     * Returns {@code true} if calls to {@link #addPossibleKey(double)} are necessary, {@code false} otherwise.
     * @return whether adding possible keys is necessary.
     */
    public abstract boolean needsPossibleKeys();

    /**
     * Adds a possible key to the structure.
     *
     * This operation is possible in the preparation mode,
     * and may throw exceptions in the initialized mode.
     *
     * @param key the possible key.
     */
    public abstract void addPossibleKey(double key);

    /**
     * Initializes the internals of the structure.
     * Since this moment, and until the next call for {@link #clear()},
     * the structure will be in an initialized mode.
     */
    public abstract void init();

    /**
     * Puts a value for the given key.
     *
     * This operation should be performed in the initialized mode,
     * and may throw exceptions in the preparation mode.
     *
     * The value must be non-negative.
     *
     * @param key the key.
     * @param value the value.
     */
    public abstract void put(double key, int value);

    /**
     * Returns a maximum value, among those which were added to the structure previously
     * and which were associated with the key smaller than, or equal to, the given key.
     *
     * This function supports returning imprecise answers with greater speed.
     * If one specifies the parameter {@code minimumMeaningfulAnswer} greater than {@code -1},
     * the search may return faster when it encounters that the answer will be smaller than the given parameter.
     * In this case, it will return {@code minimumMeaningfulAnswer - 1} or a smaller value.
     *
     * A safe value for {@code minimumMeaningfulAnswer} is -1; in this case, no information is lost.
     *
     * This operation should be performed in the initialized mode,
     * and may throw exceptions in the preparation mode.
     *
     * @param key the key.
     * @param minimumMeaningfulAnswer the minimum meaningful answer to return.
     * @return the maximum found value for another key above this key.
     */
    public abstract int getMaximumWithKeyAtMost(double key, int minimumMeaningfulAnswer);

    /**
     * Clears the existing mappings and moves the data structure to the preparation mode.
     */
    public abstract void clear();

    /**
     * Returns {@code true} if the data structure is in the initialized mode
     * and {@code false} in the preparation mode.
     * @return whether the data structure is in the initialized mode.
     */
    public abstract boolean isInitialized();
}
