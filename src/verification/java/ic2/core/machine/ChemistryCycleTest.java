package ic2.core.machine;

public final class ChemistryCycleTest {
    public static void main(String[] args) {
        int gas = 0;
        for (int left = 160; left > 0; left--) gas += GasifierCycle.step(left, true, false).gas();
        require(gas == 1000, "stone gasifier produces 1000 mB per log");
        gas = 0;
        for (int left = 80; left > 0; left--) gas += GasifierCycle.step(left, true, true).gas();
        require(gas == 810, "electric gasifier preserves original completion bonus");
        require(GasifierCycle.step(40, false, true).remaining() == 40 && GasifierCycle.step(40, false, true).gas() == 0, "power loss pauses log processing");
        var cycle = RefiningCycle.step(249, 250, 20, 20, true, true);
        require(cycle.complete() && cycle.energy() == 0 && cycle.progress() == 0, "last refinery tick spends EU and completes");
        cycle = RefiningCycle.step(30, 250, 20, 19, true, true);
        require(cycle.progress() == 29 && cycle.energy() == 19, "power loss decays progress without consuming inputs");
        require(!RefiningCycle.step(249, 250, 20, 100, true, false).complete(), "blocked output prevents commit");
        require(RefiningCycle.step(30, 250, 20, 100, false, true).progress() == 0, "invalid recipe resets progress");
        var ageing = WhiskyCycle.step(WhiskyCycle.STAGE_TICKS - 1, 9, true);
        require(ageing.stage() == 10 && ageing.age() == 0 && WhiskyCycle.grade(ageing.stage()) == 1, "first distillable whisky year starts at ten stages");
        require(WhiskyCycle.step(100, 10, false).age() == 100, "open barrel stops ageing");
        require(WhiskyCycle.evaporated(16000, 10, 16) == 14400, "opening applies original age evaporation");
        require(WhiskyCycle.evaporated(16000, 10, 8) == 7200, "grist concentration affects yield");
        require(WhiskyCycle.alcohol(0) == 0 && WhiskyCycle.alcohol(1) == 10000 && WhiskyCycle.alcohol(5) == 50000, "whisky distillation follows original year yields");
        System.out.println("Gasifier, refinery and whisky cycles: all checks passed");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
