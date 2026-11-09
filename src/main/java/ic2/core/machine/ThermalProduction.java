package ic2.core.machine;

final class ThermalProduction {
    static final int TANK_CAPACITY = 20_000;
    static final int ENERGY_PER_MB = 30;
    static final int FUEL_PER_TICK = 3;
    static final int MAX_OUTPUT = ENERGY_PER_MB * FUEL_PER_TICK;
    static final int HEAT_PER_EU = 2000;
    static final int DISPLAY_UNITS_PER_EU = 100;
    static final int SAMPLE_INTERVAL = 200;
    static final int CHARGE_PACKET = 128;

    private ThermalProduction() {}

    static int fuelOutput(int lava) {
        return Math.min(lava, FUEL_PER_TICK) * ENERGY_PER_MB;
    }

    static int heatPoints(int rawHeat, float temperature) {
        return (int) (Math.max(0, rawHeat) * Math.clamp(temperature, 0.5F, 1.5F));
    }
}
