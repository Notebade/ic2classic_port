package ic2.core.machine;

final class SteamTunnelCycle {
    record Step(int energy, int steam, int water, int remainder) {}
    static Step step(int energy, int steam, int water, int remainder) {
        // Limit each whole steam batch to available EU space; do not discard steam on overflow.
        int batches = Math.max(0, Math.min(4, Math.min(steam / 1024, (10000 - energy) / 512)));
        int numerator = remainder + batches * 640;
        return new Step(energy + batches * 512, steam - batches * 1024,
            Math.min(2000, water + numerator / 100), numerator % 100);
    }
}
