package ru.ifmo.nds.jfb.hybrid.tuning;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Random;

public class ThresholdTunerRunner {
    private static final double BUDGET_CONST = 50;
    private static final double TAKEN_CONST = 2;

    public static void main(String[] args) throws FileNotFoundException {
        Random rand = new Random();
        int countRuns = Integer.parseInt(args[0]);
        ThresholdFactory thresholdFactory = new DynamicThresholdFactory(Integer.parseInt(args[1]));
        Threshold threshold = thresholdFactory.createThreshold();
        try (PrintWriter pw = new PrintWriter(thresholdFactory.getDescription() + ".csv")) {
            pw.println("i,n,budget,taken,thr");
            for (int i = 0; i < countRuns; i++) {
                int problemSize = rand.nextInt(threshold.getThreshold());
                int operationBudget = getBudget(problemSize);
                int operationTaken = getTaken(problemSize);
                boolean wasOK = !threshold.shallTerminate(operationBudget, operationTaken);
                threshold.recordPerformance(problemSize, operationBudget, operationTaken, !wasOK);
                pw.println((i + 1) + ", " + problemSize + ", " + operationBudget + ", " + operationTaken + ", " + threshold.getThreshold());
            }
        }
    }

    private static int getTaken(int problemSize) {
        return (int) (TAKEN_CONST * problemSize * problemSize);
    }

    private static int getBudget(int problemSize) {
        return (int) (BUDGET_CONST * problemSize * Math.log(problemSize + 1));
    }
}
