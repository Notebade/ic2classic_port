package ic2.core.machine;

public final class SlagGeneratorCycleTest {
    public static void main(String[] args) {
        check(SlagGeneratorCycle.fuelTicks(1600, false) == 100, "coal uses furnace duration divided by sixteen");
        check(SlagGeneratorCycle.fuelTicks(350, true) == 2, "scrap fuel is shortened tenfold again");
        check(SlagGeneratorCycle.fuelTicks(15, false) == 0, "tiny fuels cannot create energy");
        var full = SlagGeneratorCycle.step(1, 8000, 1, false);
        check(full.remaining() == 0 && full.energy() == 8000 && full.scrap(), "burning continues at a full buffer and still makes scrap");
        check(!SlagGeneratorCycle.step(1, 0, 1, true).scrap(), "scrap fuel never makes new scrap");
        check(SlagGeneratorCycle.canRefuel(7940) && !SlagGeneratorCycle.canRefuel(7941), "new fuel waits for space for one output packet");
        int fuel = 100, energy = 0, counter = 80, scraps = 0;
        while (fuel > 0) {
            var step = SlagGeneratorCycle.step(fuel, energy, counter, false);
            fuel = step.remaining(); energy = step.energy(); counter = step.counter();
            if (step.scrap()) { scraps++; counter = 80; }
        }
        check(energy == 6000 && scraps == 1, "one coal generates 6000 EU with scheduled byproduct");
        System.out.println("Slag generator cycle: all checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
