package ic2.core.machine;

final class OceanProduction {
    static final int SAMPLE_INTERVAL = 512;
    static final int OUTPUT_LIMIT = 100;
    static final int RADIUS = 5;

    private int water;
    private int coral;
    private int generators = 1;

    // ConnectedSpace only calls this once per visited position and skips the origin.
    boolean recordAndTraverse(boolean sourceWater, boolean livingCoral, boolean generator) {
        if (sourceWater) {
            water++;
        }
        if (livingCoral) {
            coral++;
        }
        if (generator) {
            generators++;
        }
        return sourceWater || livingCoral || generator;
    }

    int output() {
        return output(water, coral, generators);
    }

    static int output(int water, int coral, int generators) {
        float waterFactor = Math.min(1.0F, water * 0.001F);
        float coralFactor = Math.min(1.0F, (coral - 25) * 0.04F);
        return (int) (Math.max(0.0F, waterFactor * coralFactor * OUTPUT_LIMIT) / generators);
    }
}
