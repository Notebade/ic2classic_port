package ic2.core.machine;

public final class InductionCycleTest {
    public static void main(String[] args) {
        var idle = InductionCycle.step(100, 50, 0, false, false, 15, 120000);
        check(idle.heat() == 96 && idle.spent() == 0 && idle.progress() == 50, "unpowered cooling preserves work");
        var hold = InductionCycle.step(9999, 0, 1, false, true, 15, 120000);
        check(hold.heat() == 10000 && hold.spent() == 1 && !hold.processing(), "redstone heats without a recipe");
        var starved = InductionCycle.step(1000, 500, 15, true, false, 15, 120000);
        check(starved.spent() == 1 && starved.progress() == 500, "heating energy is charged before processing");
        var cold = InductionCycle.step(0, 0, 16, true, false, 15, 120000);
        check(cold.heat() == 1 && cold.progress() == 1 && cold.spent() == 16, "cold startup spends 16 EU");
        check(cycles(15, 120000) == 12, "hot ordinary furnace completes in 12 ticks");
        check(cycles(29, 60000) == 6, "hot blast furnace and smoker complete in 6 ticks");
        var full = InductionCycle.step(10000, 119999, 16, true, false, 15, 120000);
        check(full.finished() && full.progress() == 0, "completed batches reset progress");
        check(InductionCycle.step(2, 0, 0, false, false, 15, 120000).heat() == 0, "cooling clamps at zero");
        check(InductionCycle.step(10000, Integer.MAX_VALUE - 1, 16, true, false, 15, Integer.MAX_VALUE).finished(),
            "long data-pack recipes must not overflow progress");
        System.out.println("Induction cycle: all checks passed");
    }

    private static int cycles(int cost, int units) {
        int progress = 0;
        for (int ticks = 1; ticks <= 20; ticks++) {
            var next = InductionCycle.step(10000, progress, 10000, true, false, cost, units);
            check(next.spent() == cost + 1, "processing includes heater cost");
            if (next.finished()) return ticks;
            progress = next.progress();
        }
        throw new AssertionError("batch never finished");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
