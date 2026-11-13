package ic2.core.machine;

final class UtilityCycles {
    private UtilityCycles() {}

    static int fenceCharge(int charge, int energy, boolean upgraded) {
        int missing = 15 - charge;
        if (missing <= 0) return 0;
        return Math.min(missing, energy / (upgraded ? Math.min(100, missing * 10) : missing));
    }

    static int teslaDamage(int energy) { return energy / 400; }
}
