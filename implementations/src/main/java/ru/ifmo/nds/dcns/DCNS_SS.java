package ru.ifmo.nds.dcns;

public final class DCNS_SS extends DCNSBase {
    public DCNS_SS(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    final int findRank(int targetFrom, int targetUntil, int pointIndex) {
        for (int target = targetFrom; target < targetUntil; ++target) {
            if (checkIfDoesNotDominate(target, pointIndex)) {
                return target;
            }
        }
        return targetUntil;
    }

    @Override
    public String getName() {
        return "DCNS-SS";
    }
}
