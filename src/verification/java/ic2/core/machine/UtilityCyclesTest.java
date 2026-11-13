package ic2.core.machine;

public final class UtilityCyclesTest {
    public static void main(String[] args) {
        if (UtilityCycles.fenceCharge(0, 100, false) != 6) throw new AssertionError("source charge divisor is missing charge");
        if (UtilityCycles.fenceCharge(15, 100, false) != 0) throw new AssertionError("full fence takes no energy");
        if (UtilityCycles.fenceCharge(0, 1000, true) != 10) throw new AssertionError("upgraded divisor caps at 100");
        if (UtilityCycles.teslaDamage(399) != 0 || UtilityCycles.teslaDamage(50000) != 125) throw new AssertionError("Tesla spends 400 EU per damage");
        System.out.println("Utility machine cycles: all checks passed");
    }
}
