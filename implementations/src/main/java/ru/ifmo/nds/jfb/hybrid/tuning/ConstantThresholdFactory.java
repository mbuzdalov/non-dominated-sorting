package ru.ifmo.nds.jfb.hybrid.tuning;

/**
 * A threshold factory that produces thresholds returning the same fixed threshold value.
 *
 * @author Irene Petrova
 * @author Maxim Buzdalov
 */
public final class ConstantThresholdFactory extends ThresholdFactory {
    private final Threshold instance;

    public ConstantThresholdFactory(int thresholdValue) {
        this.instance = new Impl(thresholdValue);
    }

    @Override
    public String getDescription() {
        return String.valueOf(instance.getThreshold());
    }

    @Override
    public Threshold createThreshold() {
        return instance;
    }

    private static final class Impl extends Threshold {
        private final int thresholdValue;

        private Impl(int thresholdValue) {
            this.thresholdValue = thresholdValue;
        }

        @Override
        public final boolean shallTerminate(int operationBudget, int operationsTaken) {
            return false;
        }

        @Override
        public final int getThreshold() {
            return thresholdValue;
        }

        @Override
        public final void recordPerformance(int problemSize, int operationBudget, int operationsTaken, boolean forced) {}
    }
}
