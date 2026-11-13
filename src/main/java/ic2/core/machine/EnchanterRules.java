package ic2.core.machine;

final class EnchanterRules {
    private EnchanterRules() {}
    static int enchantLevel(int shelves, int lapis, int bookSlots) { return Math.clamp(Math.min(shelves, Math.min(lapis, bookSlots) * 10), 0, 30); }
    static int enchantXp(int level, boolean enchanted) {
        if (level <= 0) return 0;
        int base = 7 + ((level + 9) / 10 - 1) * 2;
        return (int) Math.pow(base, enchanted ? 2.6 : 2.3);
    }
    private static long rawXp(int level) {
        if (level <= 15) return (long) level * level + 6L * level;
        return (long) (level <= 30 ? level * (double) level * 2.5 - 40.5 * level + 360 : level * (double) level * 4.5 - 162.5 * level + 2220);
    }
    static int xpForLevel(int level) { return (int) Math.clamp(rawXp(Math.max(0, level)), 0, Integer.MAX_VALUE); }
    static int levelForXp(int xp) {
        int low = 0, high = 40000;
        while (low < high) {
            int middle = (low + high + 1) / 2;
            if (rawXp(middle) <= Math.max(0, xp)) low = middle; else high = middle - 1;
        }
        return low;
    }
}
