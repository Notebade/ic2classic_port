package ic2.core.machine;

record WaterMillTier(int output, int passiveNumerator, int passiveDenominator) {
    static final int UNITS_PER_EU = 100;
    static final int MAX_SOURCES = 26;
    static final WaterMillTier BASE = new WaterMillTier(1, 2, 1);
    static final WaterMillTier LV = new WaterMillTier(4, 3, 2);

    int passiveUnits(int sources) {
        return (sources * passiveNumerator / passiveDenominator) * output;
    }
}
