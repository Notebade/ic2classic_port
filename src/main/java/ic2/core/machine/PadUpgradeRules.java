package ic2.core.machine;

final class PadUpgradeRules {
    private PadUpgradeRules() {}

    static boolean has(int flags, int index) { return (flags & 1 << index) != 0; }

    static double range(double base, int flags) {
        for (int i = 0; i < 3; i++) if (has(flags, i + 5)) base += i + 1;
        return base;
    }

    static int firstSlot(int selected, int flags) {
        return has(flags, 2) ? Math.max(0, selected - 1) : has(flags, 3) ? 0 : selected;
    }

    static int lastSlot(int selected, int flags) {
        return has(flags, 2) ? Math.min(8, selected + 1) : has(flags, 3) ? 8 : selected;
    }

    static boolean drain(int tier, int flags, boolean pvp) { return tier > 2 && has(flags, 1) && pvp; }
}
