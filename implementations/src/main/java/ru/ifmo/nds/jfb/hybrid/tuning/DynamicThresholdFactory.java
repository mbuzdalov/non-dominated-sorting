package ru.ifmo.nds.jfb.hybrid.tuning;

public class DynamicThresholdFactory extends ThresholdFactory {
    private static final double TUNING_FAILURE_PROPORTION = 0.1;
    private static final double TUNING_SUCCESS_PROPORTION = 0.1;

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
        public synchronized final int getThreshold() {
            return (int) threshold;
        }

        @Override
        public final boolean shallTerminate(int operationBudget, int operationsTaken) {
            return operationsTaken > operationBudget + operationBudget;
        }

        @Override
        public synchronized final void recordPerformance(int problemSize, int operationBudget, int operationsTaken, boolean forced) {
            // Note that the threshold, with which these measurements were made, can be different
            //   compared to the current one, which can happen in multi-threaded settings.
            // However, problemSize <= original threshold.

            double thresholdEstimate = problemSize * Math.max(0.5, Math.min(2, (double) operationBudget / operationsTaken));
            if (operationBudget >= operationsTaken) {
                // [OK, threshold can only grow]

                // Here, problemSize <= thresholdEstimate and problemSize <= original threshold,
                //   so the relation of the estimate to the original value of the threshold can vary.
                // Typically, when thresholdEstimate < threshold, it means that problemSize was originally too small.
                //   It can easily be as small as twice smaller than the threshold, however, it might be even worse,
                //   because the side branch of the recursion might have just pushed the threshold further
                //   (even multiple threads are not necessary!).
                // However, never growing the threshold in these conditions is harmful, as it can stuck there
                //   for infinite time. So we chose to increase the threshold slowly, in order for something to eventually happen.
                if (thresholdEstimate > threshold) {
                    double ratio = thresholdEstimate / threshold;
                    threshold *= (1 + (ratio - 1) * TUNING_SUCCESS_PROPORTION);
                } else {
                    threshold *= 1.0001;
                }
            } else {
                // [Not OK, threshold can only shrink]

                // When single-threaded, the following statements hold:
                //      problemSize <= threshold
                //      thresholdEstimate < problemSize, because we are here, and so operationBudget < operationsTaken
                // so thresholdEstimate < threshold always holds.
                // When multi-threaded, the following condition might be violated. In this case, we do nothing.
                if (thresholdEstimate < threshold) {
                    double ratio = threshold / thresholdEstimate;
                    threshold /= (1 + (ratio - 1) * TUNING_FAILURE_PROPORTION);
                }
            }
        }
    }
}
