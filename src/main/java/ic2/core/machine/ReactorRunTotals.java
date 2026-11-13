package ic2.core.machine;

/**
 * Actual simulation totals, independent of Minecraft serialization and UI.
 */
final class ReactorRunTotals {
    private long production;
    private long baseline;
    private long fuelTicks;
    private int fuelCells;

    void record(long output, long singlePulseProduction, int cells, int elapsedTicks) {
        production += Math.max(0, output);
        baseline += Math.max(0, singlePulseProduction);
        fuelCells = Math.max(fuelCells, cells);
        if (cells > 0) fuelTicks += elapsedTicks;
    }

    long production() {
        return production;
    }

    long baseline() {
        return baseline;
    }

    long fuelTicks() {
        return fuelTicks;
    }

    int fuelCells() {
        return fuelCells;
    }

    int efficiencyPermille() {
        return baseline == 0 ? -1 : (int)Math.min(Integer.MAX_VALUE, production * 1000 / baseline);
    }

    static int remainingTicks(int durability, int damage) {
        return Math.max(0, durability - damage) * 20;
    }

    void restore(long output, long base, long ticks, int cells) {
        production = Math.max(0, output);
        baseline = Math.max(0, base);
        fuelTicks = Math.max(0, ticks);
        fuelCells = Math.max(0, cells);
    }
}
