package ru.ifmo.nds.util;

import ru.ifmo.nds.util.veb.VanEmdeBoasSet;

public class VanEmdeBoasRankQueryStructure extends RankQueryStructure {
    private final VanEmdeBoasRangeHandle theOnlyHandle;

    public VanEmdeBoasRankQueryStructure(int maximumPoints) {
        theOnlyHandle = new VanEmdeBoasRangeHandle(maximumPoints);
    }

    @Override
    public RangeHandle createHandle(int storageStart, int from, int until, int[] indices, int[] keys) {
        theOnlyHandle.clear();
        return theOnlyHandle;
    }

    private static class VanEmdeBoasRangeHandle extends RangeHandle {
        private final VanEmdeBoasSet set;
        private final int[] values;

        VanEmdeBoasRangeHandle(int maximumPoints) {
            int scale = 0;
            while (maximumPoints > 1 << scale) {
                ++scale;
            }
            set = VanEmdeBoasSet.create(scale);
            values = new int[maximumPoints];
        }

        void clear() {
            set.clear();
        }

        @Override
        public void put(int key, int value) {
            int prevInc = set.prevInclusively(key);
            if (prevInc != -1 && values[prevInc] >= value) {
                return;
            }
            if (prevInc != key) {
                set.add(key);
            }
            values[key] = value;

            int next;
            while ((next = set.next(key)) < values.length && values[next] <= value) {
                set.remove(next);
                key = next; // not completely necessary due to how vEB works
            }
        }

        @Override
        public int getMaximumWithKeyAtMost(int key, int minimumMeaningfulAnswer) {
            key = set.prevInclusively(key);
            return key == -1 ? -1 : values[key];
        }
    }
}
