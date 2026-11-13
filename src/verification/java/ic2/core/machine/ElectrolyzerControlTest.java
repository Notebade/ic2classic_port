package ic2.core.machine;

public final class ElectrolyzerControlTest {
    public static void main(String[] args) {
        int capacity = 1_000_000_000;
        check(ElectrolyzerControl.mode(299999999, capacity) == ElectrolyzerControl.Mode.DISCHARGE, "below 30 percent");
        check(ElectrolyzerControl.mode(300000000, capacity) == ElectrolyzerControl.Mode.IDLE, "exactly 30 percent");
        check(ElectrolyzerControl.mode(699999999, capacity) == ElectrolyzerControl.Mode.IDLE, "below 70 percent");
        check(ElectrolyzerControl.mode(700000000, capacity) == ElectrolyzerControl.Mode.CHARGE, "exactly 70 percent without overflow");
        for (int tier = 1; tier <= 6; tier++) {
            check(ElectrolyzerControl.returnedEnergy(20000, tier) == 12000 + 2000 * tier, "original tier-dependent recovery");
            check(ElectrolyzerControl.returnedEnergy(20000, tier) <= ElectrolyzerControl.MAX_BUFFER,
                "all recovered energy fits the persistent buffer");
        }
        System.out.println("Electrolyzer thresholds: all checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
