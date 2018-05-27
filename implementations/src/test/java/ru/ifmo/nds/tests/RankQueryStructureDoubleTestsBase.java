package ru.ifmo.nds.tests;

import java.util.Arrays;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.RankQueryStructureDouble;

public abstract class RankQueryStructureDoubleTestsBase {
    protected abstract RankQueryStructureDouble createStructure(int maximumPoints);

    @Test
    public void randomSmokeTesting() {
        Random random = new Random(7276572326788L);
        for (int upperBound : new int[] { 30, 60, 1024, 2048, 4096, 8192, 16012, 32716}) {
            RankQueryStructureDouble structure = createStructure(upperBound);
            int[] indices = new int[upperBound];
            ArrayHelper.fillIdentity(indices, upperBound);
            for (int times = 0; times < 10; ++times) {
                int differentPoints = random.nextInt(upperBound) + 1;
                double[] keys = new double[differentPoints];
                for (int i = 0; i < differentPoints; ++i) {
                    keys[i] = random.nextDouble();
                }
                RankQueryStructureDouble.RangeHandle handle = structure.createHandle(0, 0, differentPoints, indices, keys);
                Arrays.sort(keys);

                int[] values = new int[differentPoints];
                Arrays.fill(values, -1);

                String upperBoundMsg = "at upper bound " + upperBound;
                for (int queries = 0; queries < 2000; ++queries) {
                    if (random.nextBoolean()) {
                        int keyIndex = random.nextInt(differentPoints);
                        int newValue = random.nextInt(100);
                        handle = handle.put(keys[keyIndex], newValue);
                        values[keyIndex] = Math.max(values[keyIndex], newValue);
                    } else {
                        int q = random.nextInt(upperBound);
                        int trueAnswer = -1;
                        for (int i = 0; i < differentPoints; ++i) {
                            if (keys[i] <= q) {
                                trueAnswer = Math.max(trueAnswer, values[i]);
                            }
                        }
                        Assert.assertEquals(upperBoundMsg, trueAnswer, handle.getMaximumWithKeyAtMost(q, -1));
                        Assert.assertEquals(upperBoundMsg, trueAnswer, handle.getMaximumWithKeyAtMost(q, trueAnswer));
                        Assert.assertTrue(upperBoundMsg, handle.getMaximumWithKeyAtMost(q, trueAnswer + 1) < trueAnswer + 1);
                        Assert.assertTrue(upperBoundMsg, handle.getMaximumWithKeyAtMost(q, 1000) < 1000);
                    }
                }
            }
        }
    }
}
