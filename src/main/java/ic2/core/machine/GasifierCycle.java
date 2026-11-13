package ic2.core.machine;

final class GasifierCycle {
    record Step(int remaining, int gas, boolean complete) {}
    static Step step(int remaining, boolean powered, boolean electric) {
        if (!powered || remaining <= 0) return new Step(remaining, 0, false);
        boolean done = remaining == 1;
        return new Step(remaining - 1, (electric ? 10 : 6) + (done ? electric ? 10 : 40 : 0), done);
    }
}
