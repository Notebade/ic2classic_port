package ic2.core.machine;

final class MachineUpgradeRules {
    private MachineUpgradeRules() {}

    static int scale(int base, double multiplier) {
        return (int) Math.clamp(base * multiplier, 0, Integer.MAX_VALUE);
    }

    static int packet(int baseTier, int extraTier) {
        int tier = (int) Math.clamp((long) baseTier + extraTier, 1, 13);
        return (int) (32L << (2 * (tier - 1)));
    }
}
