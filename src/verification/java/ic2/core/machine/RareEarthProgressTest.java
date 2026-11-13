package ic2.core.machine;

public final class RareEarthProgressTest {
    public static void main(String[] args) {
        verify(12800, 5);
        verify(32000, 2);
        verify(500, 128);
        verify(200, 320);
        var initial = new RareEarthProgress("rare_earth", 63000);
        var changed = initial.select("aluminium");
        check(changed.units() == 0, "changing dust type clears accumulated yield");
        var step = initial.add(4000);
        check(step.batches() == 1 && step.next().units() == 3000, "overflow remains for the next batch");
        var restored = new RareEarthProgress(step.next().material(), step.next().units());
        check(restored.equals(step.next()), "saved state resumes exact fractional yield");
        System.out.println("Rare earth accumulation: all checks passed");
    }

    private static void verify(int yield, int count) {
        RareEarthProgress state = new RareEarthProgress("rare_earth", 0);
        for (int i = 1; i <= count; i++) {
            var next = state.add(yield);
            check(next.batches() == (i == count ? 1 : 0), "yield crosses its threshold at the exact input count");
            state = next.next();
        }
        check(state.units() == 0, "exact recipes leave no floating-point remainder");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
