package ic2.core.machine;

public final class ChargingDistributionTest {
    public static void main(String[] args) {
        int[] split = ChargingDistribution.split(101, new int[]{25, 75, 0, 0, 0, 0}, new boolean[]{true, true, false, false, false, false});
        require(split[0] == 26 && split[1] == 75, "rounding preserves the original packet total");
        split = ChargingDistribution.split(101, new int[]{25, 75, 0, 0, 0, 0}, new boolean[]{false, true, false, false, false, false});
        require(split[1] == 101, "available outputs are renormalized");
        require(java.util.Arrays.stream(ChargingDistribution.split(100, new int[6], new boolean[]{true,true,true,true,true,true})).sum() == 0, "zero weights do not create energy");
        for (int packet = 6; packet <= 32768; packet += 97) {
            split = ChargingDistribution.split(packet, new int[]{1, 2, 7, 20, 30, 40}, new boolean[]{true,true,true,true,true,true});
            require(java.util.Arrays.stream(split).sum() == packet, "splitting conserves every packet");
        }
        require(ChargingDistribution.transfer(100, 20, 1000, 32) == 32, "item transfer is rate-limited");
        require(ChargingDistribution.transfer(100, 990, 1000, 32) == 10, "last charge does not overflow an item");
        require(ChargingDistribution.transfer(7, 0, 1000, 32) == 7, "source energy limits charge");
        require(ChargingDistribution.share(10, 3) == 3 && ChargingDistribution.share(7, 2) == 3, "pad sharing carries remainders to remaining items");
        System.out.println("Charging and redirector distribution: all checks passed");
    }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
