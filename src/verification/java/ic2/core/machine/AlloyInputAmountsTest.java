package ic2.core.machine;

public final class AlloyInputAmountsTest {
    public static void main(String[] args) {
        var bronze = new AlloyInputAmounts(3, 1);
        check(bronze.fits(3, 1) && !bronze.fits(2, 1) && !bronze.fits(3, 0), "both recipe quantities are required");
        check(bronze.reversed().first() == 1 && bronze.reversed().second() == 3, "swapping ingredients swaps consumed amounts");
        var netherite = new AlloyInputAmounts(2, 2);
        check(!netherite.fits(2, 1) && netherite.fits(2, 2), "secondary ingredient count is not implicitly one");
        check(new AlloyInputAmounts(1, 3).reversed().fits(3, 1), "storage-block bronze accepts reversed slots");
        System.out.println("Alloy input quantities: all checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
