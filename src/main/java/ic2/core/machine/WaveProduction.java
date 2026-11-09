package ic2.core.machine;

import java.util.function.IntPredicate;

final class WaveProduction {
    static final int SAMPLE_INTERVAL = 256;
    static final int OUTPUT_LIMIT = 128;
    private static final int MAX_WATER_LENGTH = 50;
    private static final int BASE_OUTPUT = 15;

    private WaveProduction() {}

    static int sourceLength(IntPredicate isSource) {
        int length = 0;
        while (length < MAX_WATER_LENGTH && isSource.test(length + 1)) {
            length++;
        }
        return length;
    }

    static int target(int waterLength, boolean raining, boolean thundering, double variation) {
        double adjustedLength = waterLength;
        if (raining) {
            adjustedLength *= 1.2;
        }
        if (thundering) {
            adjustedLength *= 4;
        }
        adjustedLength *= variation;
        return (int) (adjustedLength / MAX_WATER_LENGTH * BASE_OUTPUT);
    }
}
