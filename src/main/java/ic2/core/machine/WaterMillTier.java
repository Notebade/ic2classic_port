package ic2.core.machine;

record WaterMillTier(int output, int radius, int passiveNumerator, int passiveDenominator,
                     int passiveLimit, int chargePacket) {
    static final int UNITS_PER_EU = 100;
    static final WaterMillTier BASE = new WaterMillTier(1, 1, 2, 1, 52, 32);
    static final WaterMillTier LV = new WaterMillTier(4, 1, 3, 2, 39, 32);
    static final WaterMillTier MV = new WaterMillTier(32, 2, 11, 20, 58, 128);
    static final WaterMillTier HV = new WaterMillTier(256, 2, 2, 5, 45, 512);

    int maxSources() {
        int diameter = 2 * radius + 1;
        return diameter * diameter * diameter - 1;
    }

    int passiveUnits(int sources) {
        return Math.min(passiveLimit, sources * passiveNumerator / passiveDenominator) * output;
    }
}
