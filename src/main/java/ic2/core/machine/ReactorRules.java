package ic2.core.machine;

final class ReactorRules {
    static final int ROWS = 6;
    static final int COLUMNS = 9;
    static final int BASE_COLUMNS = 3;
    static final int SLOT_COUNT = ROWS * COLUMNS;
    static final int MAX_HEAT = 10_000;
    static final int RADIATION_HEAT = MAX_HEAT * 7 / 10;
    static final int CYCLE_TICKS = 20;
    static final int VENT_COOLING = 6;
    static final int EU_PER_PULSE = 20;

    private ReactorRules() {}

    static int energy(int cells, int neighboringRods) {
        return cells * pulsesPerCell(cells, neighboringRods) * EU_PER_PULSE;
    }

    static int heatPerCell(int cells, int neighboringComponents) {
        int pulses = pulsesPerCell(cells, neighboringComponents);
        return pulses * (pulses + 1) * 2;
    }

    private static int pulsesPerCell(int cells, int neighboringComponents) {
        return 1 + cells / 2 + neighboringComponents;
    }

    static int[] neighbors(int slot, int width) {
        int column = slot % COLUMNS;
        return new int[] {
            column > 0 ? slot - 1 : -1,
            column + 1 < width ? slot + 1 : -1,
            slot >= COLUMNS ? slot - COLUMNS : -1,
            slot < SLOT_COUNT - COLUMNS ? slot + COLUMNS : -1
        };
    }
}
