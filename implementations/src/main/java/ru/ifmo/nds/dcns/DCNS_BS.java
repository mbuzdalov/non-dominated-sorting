package ru.ifmo.nds.dcns;

public final class DCNS_BS extends DCNSBase {
    public DCNS_BS(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    final int findRank(int targetFrom, int targetUntil, int pointIndex) {
        if (checkIfDoesNotDominate(targetFrom, pointIndex)) {
            return targetFrom;
        }
        while (targetUntil - targetFrom > 1) {
            int mid = (targetFrom + targetUntil) >>> 1;
            if (checkIfDoesNotDominate(mid, pointIndex)) {
                targetUntil = mid;
            } else {
                targetFrom = mid;
            }
        }
        return targetUntil;
    }

    @Override
    public String getName() {
        return "DCNS-BS";
    }
}
