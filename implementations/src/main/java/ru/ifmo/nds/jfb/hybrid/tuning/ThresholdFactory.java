package ru.ifmo.nds.jfb.hybrid.tuning;

/**
 * A factory for creating thresholds.
 *
 * The threshold itself is a stateful object, so a factory is needed to be passed to the hybrids.
 * Making a singleton object that supplies the specific thresholds seems to be a good pattern.
 *
 * @author Maxim Buzdalov
 * @author Irene Petrova
 */
public abstract class ThresholdFactory {
    /**
     * Returns the description of the threshold.
     *
     * Needs to be a constant value, as it is intended to be used in the algorithmic descriptions.
     *
     * @return the description of the threshold.
     */
    public abstract String getDescription();

    /**
     * Creates and returns a new threshold.
     *
     * The threshold instance may be the same every time provided it is safe to do.
     * The only two imaginable cases are constant thresholds and thresholds that always fail.
     *
     * @return the new threshold.
     */
    public abstract Threshold createThreshold();
}
