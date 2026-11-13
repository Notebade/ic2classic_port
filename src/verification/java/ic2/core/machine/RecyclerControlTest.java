package ic2.core.machine;

public final class RecyclerControlTest {
    public static void main(String[] args) {
        int[] chances = {100, 84, 71, 59, 50};
        for (int mode = 1; mode <= 5; mode++) {
            check(RecyclerControl.chancePercent(mode, 4) == chances[mode - 1], "original grinder probabilities");
            check(RecyclerControl.chancePercent(mode, 1) == 100, "guaranteed recipes stay guaranteed");
            int progress = 0, energy = 0, ticks = 0;
            while (true) {
                var step = RecyclerControl.slowStep(progress, 1000, mode, 100, true);
                progress = step.progress();
                energy += step.spent();
                ticks++;
                if (step.finished()) break;
                if (ticks > 100) throw new AssertionError("grinder never completes");
            }
            check(ticks == (100 + mode - 1) / mode && energy == ticks * mode * 10, "speed and EU are coupled");
        }
        check(RecyclerControl.slowStep(40, 49, 5, 100, true).progress() == 40, "insufficient EU preserves work");
        check(RecyclerControl.slowStep(40, 1000, 5, 100, false).spent() == 0, "invalid input spends no EU");
        check(RecyclerControl.compactDuration(true) == 719280, "scrap-box recipe has original additive time modifier");
        check(RecyclerControl.compactDuration(false) == 9990, "ordinary compacting cycle finishes in one hot tick");
        for (int slot = 2; slot <= 9; slot++) {
            check(RecyclerControl.nextQueueSlot(slot) == (slot == 9 ? 1 : slot + 1), "queue rotates into the active slot");
        }
        check(new ProcessingLayout(9, 1).processingPasses() == 1, "queue never multiplies recipe processing");
        check(RecyclerControl.queueMove(64, 60, 64, true) == 4, "queue transfer respects stack room");
        check(RecyclerControl.queueMove(64, 64, 64, true) == 0, "full target stays unchanged");
        check(RecyclerControl.queueMove(64, 1, 64, false) == 0, "different items never merge");
        int moved = RecyclerControl.queueMove(37, 12, 64, true);
        check(moved == 37, "the complete source fits into the target's remaining room");
        System.out.println("Recycler modes and queue: all checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
