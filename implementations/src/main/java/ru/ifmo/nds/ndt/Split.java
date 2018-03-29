package ru.ifmo.nds.ndt;

class Split {
    int coordinate;
    double value;
    Split good, weak;

    void initialize(int coordinate, double value, Split good, Split weak) {
        this.coordinate = coordinate;
        this.value = value;
        this.good = good;
        this.weak = weak;
    }

    static class NullMaxDepth extends Split {
        static Split INSTANCE = new NullMaxDepth();

        private NullMaxDepth() {
        }
    }

    static class NullPoints extends Split {
        static Split INSTANCE = new NullPoints();

        private NullPoints() {
        }
    }
}
