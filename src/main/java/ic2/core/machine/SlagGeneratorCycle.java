package ic2.core.machine;

final class SlagGeneratorCycle {
    static final int OUTPUT = 60;
    static final int CAPACITY = 8000;

    private SlagGeneratorCycle() {}

    static int fuelTicks(int rawFuel, boolean scrap) { return Math.max(0, rawFuel) / 16 / (scrap ? 10 : 1); }
    static boolean canRefuel(int energy) { return energy <= CAPACITY - OUTPUT; }

    static Step step(int remaining, int energy, int counter, boolean scrapFuel) {
        return step(remaining, energy, counter, scrapFuel, OUTPUT);
    }

    static Step step(int remaining, int energy, int counter, boolean scrapFuel, int output) {
        if (remaining <= 0) return new Step(0, energy, counter, false, false);
        int nextCounter = scrapFuel ? counter : counter - 1;
        return new Step(remaining - 1, Math.min(CAPACITY, energy + output), nextCounter, !scrapFuel && nextCounter <= 0, true);
    }

    record Step(int remaining, int energy, int counter, boolean scrap, boolean burning) {}
}
