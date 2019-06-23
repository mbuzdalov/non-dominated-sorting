package ru.ifmo.nds.jfb.hybrid.tuning;

public class DynamicAdjustableIncreaseThresholdFactory extends ThresholdFactory {
    private static final double TUNING_FAILURE_PROPORTION = 0.1;
    private static final double TUNING_SUCCESS_PROPORTION = 0.1;

    private final int initialValue;

    public DynamicAdjustableIncreaseThresholdFactory(int initialValue) {
        this.initialValue = initialValue;
    }

    @Override
    public String getDescription() {
        return "dynamic adjust increase (initially " + initialValue + ")";
    }

    @Override
    public Threshold createThreshold() {
        return new DynamicAdjustableIncreaseThresholdFactory.Impl(initialValue);
    }

    private static final class Impl extends Threshold {

        private static final double MAX_MULTIPLE = 1.01;
        private static final double MIN_MULTIPLE = 1.000001;

        private double multiple = 1.01;
        private boolean isSequence = false;
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
        public synchronized final void recordPerformance(int problemSize, int operationBudget, int operationsTaken, boolean forced) {
            double thresholdEstimate = problemSize * Math.max(0.5, Math.min(2, (double) operationBudget / operationsTaken));
            if (operationBudget >= operationsTaken) {
                // OK, threshold can only grow
                if (thresholdEstimate > threshold) {
                    double ratio = thresholdEstimate / threshold;
                    threshold *= (1 + (ratio - 1) * TUNING_SUCCESS_PROPORTION);
                } else {
                    threshold *= multiple;
                    if (isSequence) {
                        multiple = Math.max(1 + (multiple - 1) / 2, MIN_MULTIPLE);
                    }
                    isSequence = true;
                }
            } else {
                isSequence = false;
                multiple = MAX_MULTIPLE;
                // Not OK, threshold can only shrink
                if (thresholdEstimate < threshold) {
                    double ratio = threshold / thresholdEstimate;
                    threshold /= (1 + (ratio - 1) * TUNING_FAILURE_PROPORTION);
                } else {
                    threshold /= 1.01;
                }
            }
        }

//        @Override
//        public final void recordPerformance(int problemSize, int operationBudget, int operationsTaken, boolean forced) {
//            double thresholdEstimate = problemSize * Math.max(0.5, Math.min(2, (double) operationBudget / operationsTaken));
//            if (operationBudget >= operationsTaken) {
//                // OK, threshold can only grow
//                if (thresholdEstimate > threshold) {
//                    double ratio = thresholdEstimate / threshold;
//                    threshold *= (1 + (ratio - 1) * TUNING_SUCCESS_PROPORTION);
//                } else {
//                    threshold *= 1.0001;
//                }
//            } else {
//                // Not OK, threshold can only shrink
//                if (thresholdEstimate < threshold) {
//                    double ratio = threshold / thresholdEstimate;
//                    threshold /= (1 + (ratio - 1) * TUNING_FAILURE_PROPORTION);
//                } else {
//                    threshold /= 1.00001;
//                }
//            }
//        }
    }
}

