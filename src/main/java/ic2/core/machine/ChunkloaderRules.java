package ic2.core.machine;

final class ChunkloaderRules {
    private static final int[] COST = {5, 15, 60, 300};
    private ChunkloaderRules() {}
    static int cost(int radius, boolean processing) { return COST[Math.clamp(radius, 0, 3)] * (processing ? 2 : 1); }
    static int startup(int radius, boolean processing) { return Math.min(30000, cost(radius, processing) * 100); }
}
