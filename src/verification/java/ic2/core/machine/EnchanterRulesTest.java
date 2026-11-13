package ic2.core.machine;

public final class EnchanterRulesTest {
    public static void main(String[] args) {
        if (EnchanterRules.enchantLevel(30, 3, 1) != 10 || EnchanterRules.enchantLevel(30, 2, 3) != 20
                || EnchanterRules.enchantLevel(17, 3, 3) != 17) throw new AssertionError("lapis, books and shelves independently cap level");
        if (EnchanterRules.enchantXp(10, false) != 87 || EnchanterRules.enchantXp(20, false) != 156
                || EnchanterRules.enchantXp(30, false) != 248 || EnchanterRules.enchantXp(30, true) != 510)
            throw new AssertionError("source plain and already-enchanted XP costs");
        int[] levels = {0,15,16,30,31,32};
        int[] totals = {0,315,352,1395,1507,1628};
        for (int index = 0; index < levels.length; index++) {
            if (EnchanterRules.xpForLevel(levels[index]) != totals[index] || EnchanterRules.levelForXp(totals[index]) != levels[index])
                throw new AssertionError("XP conversion across vanilla level brackets");
        }
        if (EnchanterRules.levelForXp(1506) != 30) throw new AssertionError("partial level is retained on XP withdrawal");
        System.out.println("Electric enchanter rules: all checks passed");
    }
}
