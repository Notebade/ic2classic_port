package ic2.core.machine;

final class WindmillCycle {
    static final int DIVISOR = 256000;
    record State(double speed, int remainder) {
        Step step(double target, boolean rotor, boolean openAir, int energy) {
            if (!rotor) return new Step(new State(0, remainder), energy);
            double next = Math.max(0, speed + (openAir ? Math.clamp((target - speed) * .01, -.01, .01) : -.001));
            int stored = remainder;
            if (energy < 4000) stored += (int) (next * DIVISOR);
            if (stored > DIVISOR && energy < 4000) {
                energy = Math.min(4000, energy + stored / DIVISOR * 256);
                stored %= DIVISOR;
            }
            return new Step(new State(next, stored), energy);
        }
    }
    record Step(State next, int energy) {}
    record Area(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {}
    static Area area(int frontX, int frontZ, int radius) {
        if (Math.abs(frontX) + Math.abs(frontZ) != 1) throw new IllegalArgumentException("Horizontal windmill required");
        int half = radius / 2;
        return new Area(frontX == 0 ? -half : Math.min(frontX * 20, -frontX * 5), -half,
            frontZ == 0 ? -half : Math.min(frontZ * 20, -frontZ * 5),
            frontX == 0 ? half : Math.max(frontX * 20, -frontX * 5), half,
            frontZ == 0 ? half : Math.max(frontZ * 20, -frontZ * 5));
    }
    static double freeRatio(int connected, int volume, int mills) {
        return volume <= 150 ? 0 : Math.clamp(connected / (volume - 150.0), 0, 1) / Math.max(1, mills);
    }
}
