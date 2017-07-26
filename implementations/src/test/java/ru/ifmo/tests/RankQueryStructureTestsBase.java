package ru.ifmo.tests;

import org.junit.Assert;
import org.junit.Test;
import ru.ifmo.util.RankQueryStructure;

import java.util.Arrays;
import java.util.Random;

public abstract class RankQueryStructureTestsBase {
    protected abstract RankQueryStructure createStructure(int maximumPoints);

    @Test
    public void randomSmokeTesting() {
        Random random = new Random();
        int upperBound = random.nextInt(100) + 100;
        RankQueryStructure structure = createStructure(upperBound);
        for (int times = 0; times < 1000; ++times) {
            int differentPoints = random.nextInt(upperBound) + 1;

            double[] keys = new double[differentPoints];
            for (int i = 0; i < differentPoints; ++i) {
                keys[i] = random.nextDouble();
                structure.addPossibleKey(keys[i]);
            }
            Arrays.sort(keys);

            int[] values = new int[differentPoints];
            Arrays.fill(values, -1);

            structure.init();

            for (int queries = 0; queries < 1000; ++queries) {
                if (random.nextBoolean()) {
                    int keyIndex = random.nextInt(differentPoints);
                    int newValue = random.nextInt(100);
                    structure.put(keys[keyIndex], newValue);
                    values[keyIndex] = Math.max(values[keyIndex], newValue);
                } else {
                    double q = random.nextDouble();
                    int trueAnswer = -1;
                    for (int i = 0; i < differentPoints; ++i) {
                        if (keys[i] <= q) {
                            trueAnswer = Math.max(trueAnswer, values[i]);
                        }
                    }
                    Assert.assertEquals(trueAnswer, structure.getMaximumWithKeyAtMost(q));
                }
            }

            structure.clear();
        }
    }
}
