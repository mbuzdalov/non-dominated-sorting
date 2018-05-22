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
        public RangeHandle put(int key, int value) {
            set.setEnsuringMonotonicity(key, 0, value, values);
            return this;
        }

        @Override
        public int getMaximumWithKeyAtMost(int key, int minimumMeaningfulAnswer) {
            key = set.prevInclusively(key);
            return key == -1 ? -1 : values[key];
        }
    }
}
