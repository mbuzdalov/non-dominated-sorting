package ru.ifmo.nds.tests;

import java.util.BitSet;
import java.util.Random;

import org.junit.Assert;
import org.junit.Test;

import ru.ifmo.nds.util.veb.VanEmdeBoasSet;

public class VanEmdeBoasSetTest {
    @Test
    public void generatedTest1() {
        VanEmdeBoasSet veb = VanEmdeBoasSet.create(7);
        BitSet bit = new BitSet(1 << 7);
        int[] mod = { 100, 57, 125, 67 };
        for (int m : mod) {
            if (m >= 0) {
                veb.add(m);
                bit.set(m);
            } else {
                veb.remove(-m - 1);
                bit.clear(-m - 1);
            }
        }
        Assert.assertEquals(bit.previousSetBit(66 - 1), veb.prev(66));
    }

    @Test
    public void smokeTest() {
        Random random = new Random(8243963462377347L);

        for (int scale = 1; scale < 20; ++scale) {
            VanEmdeBoasSet veb = VanEmdeBoasSet.create(scale);
            BitSet bit = new BitSet(1 << scale);
            int queries = (1 << scale) * 4;
            for (int t = 0; t < queries; ++t) {
                int index = random.nextInt(1 << scale);
                switch (random.nextInt(5)) {
                    case 0: {
                        veb.add(index);
                        bit.set(index);
                        break;
                    }
                    case 1: {
                        veb.remove(index);
                        bit.clear(index);
                        break;
                    }
                    case 2: {
                        Assert.assertEquals(bit.get(index), veb.contains(index));
                        break;
                    }
                    case 3: {
                        Assert.assertEquals(bit.nextSetBit(index + 1), veb.next(index));
                        break;
                    }
                    case 4: {
                        Assert.assertEquals(bit.previousSetBit(index - 1), veb.prev(index));
                        break;
                    }
                }
            }
        }
    }
}
