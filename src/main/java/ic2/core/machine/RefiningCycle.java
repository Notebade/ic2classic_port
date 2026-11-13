package ic2.core.machine;

final class RefiningCycle {
    record Step(int progress, int energy, boolean complete, boolean active) {}
    static Step step(int progress, int duration, int cost, int energy, boolean recipe, boolean room) {
        if (!recipe) return new Step(0, energy, false, false);
        if (!room || energy < cost) return new Step(Math.max(0, progress - 1), energy, false, false);
        boolean complete = progress + 1 >= duration;
        return new Step(complete ? 0 : progress + 1, energy - cost, complete, true);
    }
}
