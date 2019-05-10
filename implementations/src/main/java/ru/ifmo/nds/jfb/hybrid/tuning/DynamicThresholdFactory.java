package ru.ifmo.nds.jfb.hybrid.tuning;

public class DynamicThresholdFactory extends ThresholdFactory {
    private static final double TUNING_FAILURE_EXPONENT = 0.5;
    private static final double TUNING_SUCCESS_EXPONENT = 0.02;

    private final int initialValue;

    public DynamicThresholdFactory(int initialValue) {
        this.initialValue = initialValue;
    }

    @Override
    public String getDescription() {
        return "dynamic (initially " + initialValue + ")";
    }

    @Override
    public Threshold createThreshold() {
        return new Impl(initialValue);
    }

    private static final class Impl extends Threshold {
        private double threshold;

        Impl(int initialValue) {
            threshold = initialValue;
        }

        @Override
        public final int getThreshold() {
            return (int) threshold;
        }

        @Override
        public final boolean shallTerminate(int operationBudget, int operationsTaken) {
            return operationsTaken > operationBudget + operationBudget;
        }

        @Override
        public final void recordPerformance(int problemSize, int operationBudget, int operationsTaken, boolean forced) {
            if (operationsTaken <= operationBudget) {
                threshold *= Math.pow(Math.min(2, (double) (operationBudget) / operationsTaken), TUNING_SUCCESS_EXPONENT);
            } else {
                threshold *= Math.pow((double) (operationBudget) / operationsTaken, TUNING_FAILURE_EXPONENT);
            }
        }
    }
}
