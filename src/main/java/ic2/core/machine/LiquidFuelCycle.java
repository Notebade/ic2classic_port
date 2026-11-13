package ic2.core.machine;

final class LiquidFuelCycle {
    record State(int remaining, int production) {
        Step step(int energy) {
            return remaining > 0 && energy <= 10000 - production
                ? new Step(new State(remaining - 1, production), energy + production) : new Step(this, energy);
        }
    }
    record Step(State next, int energy) {}
    static int fuelTicks(int ticksPerBucket, int amount) { return (int) (ticksPerBucket * (amount / 1000.0F)); }
    static int packet(int production) { return production <= 32 ? 32 : 128; }
}
