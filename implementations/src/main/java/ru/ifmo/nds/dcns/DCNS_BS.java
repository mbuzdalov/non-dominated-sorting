package ru.ifmo.nds.dcns;

public final class DCNS_BS extends DCNSBase {
    public DCNS_BS(int maximumPoints, int maximumDimension) {
        super(maximumPoints, maximumDimension);
    }

    @Override
    final int findRank(int targetFrom, int targetUntil, double[] point) {
        if (checkIfDoesNotDominate(targetFrom, point)) {
            return targetFrom;
        }
        while (targetUntil - targetFrom > 1) {
            int mid = (targetFrom + targetUntil) >>> 1;
            if (checkIfDoesNotDominate(mid, point)) {
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
