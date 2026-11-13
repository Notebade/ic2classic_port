package ic2.core.machine;

/** Progress is measured in thirtieths of the original operation time. */
final class InductionCycle {
    static final int MAX_HEAT = 10000;

    private InductionCycle() {}

    static Step step(int heat, int progress, int energy, boolean work, boolean powered, int cost, int duration) {
        boolean heating = (work || powered) && energy > 0;
        int nextHeat = heating ? Math.min(MAX_HEAT, heat + 1) : Math.max(0, heat - 4);
        int spent = heating ? 1 : 0;
        boolean processing = work && heating && energy - spent >= cost;
        long nextProgress = progress;
        if (processing) {
            spent += cost;
            nextProgress += nextHeat;
        }
        boolean finished = processing && nextProgress >= duration;
        return new Step(nextHeat, finished ? 0 : (int) nextProgress, spent, processing, finished, heating);
    }

    record Step(int heat, int progress, int spent, boolean processing, boolean finished, boolean heating) {}
}
