package ru.ifmo.nds.jfb.hybrid.tuning;

/**
 * This is a base class for threshold maintainers.
 *
 * @author Maxim Buzdalov
 * @author Irene Petrova
 */
public abstract class Threshold {
    /**
     * Returns the current threshold value.
     * @return the current threshold value.
     */
    public abstract int getThreshold();

    /**
     * Decides whether it is time to terminate the algorithm.
     * The general contract is that if {@code operationsTaken <= operationBudget}, this method returns {@code false}.
     * When the budget is exceeded, it is up to the threshold to decide.
     *
     * This is an effectively pure operation, which shall not alter the threshold value.
     *
     * @param operationBudget the estimated budget, in number of operations.
     * @param operationsTaken the actual number of operations taken.
     * @return {@code true} if the algorithm shall prematurely terminate, {@code false} otherwise.
     */
    public abstract boolean shallTerminate(int operationBudget, int operationsTaken);

    /**
     * Adjusts the threshold after the given incident is recorded.
     *
     * @param problemSize the problem size on which the algorithm has just been run.
     * @param operationBudget the estimated budget, in number of operations, for the algorithm to be considered OK.
     * @param operationsTaken the actual number of operations taken. If this value does not exceed the budget,
     *                        the algorithm run is considered to be successful, otherwise failed.
     * @param forced whether the algorithm was forced to terminate. Must be {@code false} if
     *               {@code operationsTaken <= operationBudget}. If set to {@code true},
     *               {@code operationsTaken} is to be interpreted as a lower bound on the actual number of operations.
     */
    public abstract void recordPerformance(int problemSize, int operationBudget, int operationsTaken, boolean forced);
}
