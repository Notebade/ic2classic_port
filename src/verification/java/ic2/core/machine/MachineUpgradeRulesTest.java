package ic2.core.machine;

public final class MachineUpgradeRulesTest {
    public static void main(String[] args) {
        if (MachineUpgradeRules.scale(32, Math.pow(1.3, 2)) != 54) throw new AssertionError("two overclockers increase transfer");
        if (MachineUpgradeRules.scale(40000 + 20000, 2) != 120000) throw new AssertionError("storage addition precedes multiplication");
        if (MachineUpgradeRules.packet(1, 2) != 512) throw new AssertionError("two transformers add two tiers");
        if (MachineUpgradeRules.packet(4, Integer.MAX_VALUE) != 536870912) throw new AssertionError("tier bounded at 13");
        if (MachineUpgradeRules.scale(1000, Double.POSITIVE_INFINITY) != Integer.MAX_VALUE) throw new AssertionError("overflow saturates");
        System.out.println("Machine upgrade rules: all checks passed");
    }
}
