package ru.ifmo.nds.tests;

import org.junit.Assert;
import org.junit.Test;

import ru.ifmo.nds.util.ArrayHelper;
import ru.ifmo.nds.util.RankQueryStructure;

import java.util.Arrays;
import java.util.Random;

public abstract class RankQueryStructureTestsBase {
    protected abstract RankQueryStructure createStructure(int maximumPoints);

    @Test
    public void randomSmokeTesting() {
        Random random = new Random();
        int upperBound = random.nextInt(100) + 100;
        RankQueryStructure structure = createStructure(upperBound);
        int[] indices = new int[upperBound];
        ArrayHelper.fillIdentity(indices, upperBound);
        for (int times = 0; times < 1000; ++times) {
            int differentPoints = random.nextInt(upperBound) + 1;
            int[] keys = new int[differentPoints];
            for (int i = 0; i < differentPoints; ++i) {
                keys[i] = random.nextInt();
            }
            RankQueryStructure.RangeHandle handle = structure.createHandle(0, 0, differentPoints, indices, keys);
            Arrays.sort(keys);

            int[] values = new int[differentPoints];
            Arrays.fill(values, -1);

            for (int queries = 0; queries < 2000; ++queries) {
                if (random.nextBoolean()) {
                    int keyIndex = random.nextInt(differentPoints);
                    int newValue = random.nextInt(100);
                    handle.put(keys[keyIndex], newValue);
                    values[keyIndex] = Math.max(values[keyIndex], newValue);
                } else {
                    int q = random.nextInt();
                    int trueAnswer = -1;
                    for (int i = 0; i < differentPoints; ++i) {
                        if (keys[i] <= q) {
                            trueAnswer = Math.max(trueAnswer, values[i]);
                        }
                    }
                    Assert.assertEquals(trueAnswer, handle.getMaximumWithKeyAtMost(q, -1));
                    Assert.assertEquals(trueAnswer, handle.getMaximumWithKeyAtMost(q, trueAnswer));
                    Assert.assertTrue(handle.getMaximumWithKeyAtMost(q, trueAnswer + 1) < trueAnswer + 1);
                    Assert.assertTrue(handle.getMaximumWithKeyAtMost(q, 1000) < 1000);
                }
            }
        }
    }
}
