package ru.ifmo.nds.util;

import ru.ifmo.nds.util.veb.VanEmdeBoasSet;

public class VanEmdeBoasRankQueryStructure extends RankQueryStructure {
    private final EmptyOrSingleHandle smallHandle;

    public VanEmdeBoasRankQueryStructure(int maximumPoints) {
        VanEmdeBoasRangeHandle bigHandle = new VanEmdeBoasRangeHandle(maximumPoints);
        smallHandle = new EmptyOrSingleHandle(bigHandle);
    }

    @Override
    public RangeHandle createHandle(int storageStart, int from, int until, int[] indices, int[] keys) {
        smallHandle.clear();
        return smallHandle;
    }

    private static class EmptyOrSingleHandle extends RangeHandle {
        private final VanEmdeBoasRangeHandle forTwoOrMore;
        private int key = Integer.MAX_VALUE, value = Integer.MIN_VALUE;

        private EmptyOrSingleHandle(VanEmdeBoasRangeHandle forTwoOrMore) {
            this.forTwoOrMore = forTwoOrMore;
        }

        @Override
        public RangeHandle put(int key, int value) {
            if (key < this.key) {
                if (value < this.value) {
                    forTwoOrMore.put(this.key, this.value);
                    return forTwoOrMore.put(key, value);
                }
                this.key = key;
                this.value = value;
            } else if (key == this.key) {
                if (this.value < value) {
                    this.value = value;
                }
            } else {
                if (value > this.value) {
                    forTwoOrMore.put(this.key, this.value);
                    return forTwoOrMore.put(key, value);
                }
            }
            return this;
        }

        @Override
        public int getMaximumWithKeyAtMost(int key, int minimumMeaningfulAnswer) {
            return key >= this.key ? value : -1;
        }

        void clear() {
            forTwoOrMore.clear();
            this.key = Integer.MAX_VALUE;
            this.value = Integer.MIN_VALUE;
        }
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
