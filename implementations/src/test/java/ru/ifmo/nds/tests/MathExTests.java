package ru.ifmo.nds.tests;

import org.junit.Test;
import ru.ifmo.nds.util.MathEx;

import java.util.Random;

public class MathExTests {
    @Test
    public void log2upSmoke() {
        Random r = new Random(82454624363L);
        for (int t = 0; t < 1000000; ++t) {
            int length = r.nextInt(31);
            int mask = (1 << length) + (1 << length) - 1;
            int value;
            do {
                value = r.nextInt() & mask;
            } while (value == 0);
            int expected = (int) Math.ceil(Math.log(value) / Math.log(2));
            int found = MathEx.log2up(value);
            if (expected != found) {
                throw new AssertionError("On value " + value + " = " + Integer.toBinaryString(value) + " expected " + expected + " found " + found);
            }
        }
    }
}
