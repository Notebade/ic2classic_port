package ic2.core.machine;

public final class ChunkloaderRulesTest {
    public static void main(String[] args) {
        int[] cost = {5,15,60,300};
        for (int radius = 0; radius < 4; radius++) {
            if (ChunkloaderRules.cost(radius, false) != cost[radius] || ChunkloaderRules.cost(radius, true) != cost[radius] * 2)
                throw new AssertionError("source radius and ticking costs");
        }
        if (ChunkloaderRules.startup(3, true) != 30000 || ChunkloaderRules.startup(0, false) != 500)
            throw new AssertionError("startup buffer threshold");
        System.out.println("Chunkloader cycle: all checks passed");
    }
}
