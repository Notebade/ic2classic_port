package ic2.core.machine;

final class ReactorRules {
    static final int ROWS = 6;
    static final int COLUMNS = 9;
    static final int BASE_COLUMNS = 3;
    static final int SLOT_COUNT = ROWS * COLUMNS;
    static final int MAX_HEAT = 10_000;
    static final int CYCLE_TICKS = 20;
    static final int VENT_COOLING = 6;

    private ReactorRules() {}

    static int energy(int pulses) {
        return pulses * 20;
    }

    static int heat(int pulses) {
        return pulses * (pulses + 1) * 2;
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
