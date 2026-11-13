package ic2.core.machine;

public final class PadUpgradeRulesTest {
    public static void main(String[] args) {
        int fields = (1 << 5) | (1 << 6) | (1 << 7);
        if (PadUpgradeRules.range(.8, fields) != 6.8) throw new AssertionError("field variants add 1, 2 and 3 blocks once each");
        if (PadUpgradeRules.firstSlot(4, 1 << 2) != 3 || PadUpgradeRules.lastSlot(4, 1 << 2) != 5) throw new AssertionError("proximity selects adjacent hotbar slots");
        if (PadUpgradeRules.firstSlot(4, 1 << 3) != 0 || PadUpgradeRules.lastSlot(4, 1 << 3) != 8) throw new AssertionError("wide band selects full hotbar");
        if (PadUpgradeRules.firstSlot(0, 1 << 2) != 0 || PadUpgradeRules.lastSlot(8, 1 << 2) != 8) throw new AssertionError("hotbar stays bounded");
        if (PadUpgradeRules.drain(2, 1 << 1, true) || !PadUpgradeRules.drain(3, 1 << 1, true) || PadUpgradeRules.drain(3, 1 << 1, false)) throw new AssertionError("drain requires HV and PVP");
        System.out.println("Charging pad upgrades: all checks passed");
    }
}
