package ic2.core.machine;

public final class FisherRulesTest {
    public static void main(String[] args) {
        if (FisherRules.validDepth(4) || !FisherRules.validDepth(5) || !FisherRules.validDepth(10) || FisherRules.validDepth(11))
            throw new AssertionError("source chain range is five to ten blocks");
        if (FisherRules.nextProgress(25, 149) != 24 || FisherRules.nextProgress(0, 0) != 0 || FisherRules.nextProgress(299, 150) != 300)
            throw new AssertionError("source 150 EU/t gate and progress rollback");
        if (FisherRules.luck(5) != 1 || FisherRules.luck(10) != 6) throw new AssertionError("chain depth affects loot luck");
        System.out.println("Electric fisher rules: all checks passed");
    }
}
