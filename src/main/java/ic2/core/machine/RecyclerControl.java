package ic2.core.machine;

final class RecyclerControl {
    private static final int[] CHANCES = {100, 84, 71, 59, 50};

    private RecyclerControl() {}

    static int chancePercent(int mode, int denominator) {
        requireMode(mode);
        return denominator == 1 ? 100 : CHANCES[mode - 1];
    }

    static Step slowStep(int progress, int energy, int mode, int duration, boolean work) {
        requireMode(mode);
        int cost = mode * 10;
        boolean processing = work && energy >= cost;
        int next = work ? progress + (processing ? mode : 0) : 0;
        boolean finished = processing && next >= duration;
        return new Step(finished ? 0 : next, processing ? cost : 0, finished, processing);
    }

    static int compactDuration(boolean scrapBox) { return (333 + (scrapBox ? 23643 : 0)) * 30; }

    static int nextQueueSlot(int slot) {
        if (slot < 2 || slot > 9) throw new IllegalArgumentException("Invalid queue slot");
        return slot == 9 ? 1 : slot + 1;
    }

    static int queueMove(int sourceCount, int targetCount, int limit, boolean compatible) {
        return compatible ? Math.max(0, Math.min(sourceCount, limit - targetCount)) : 0;
    }

    private static void requireMode(int mode) {
        if (mode < 1 || mode > 5) throw new IllegalArgumentException("Invalid grinder mode");
    }

    record Step(int progress, int spent, boolean finished, boolean processing) {}
}
