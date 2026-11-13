package ic2.core.machine;

import java.util.ArrayList;
import java.util.List;

/** Original 2x2x2 builder: right and forward from the inward facing, then down. */
final class PressureStructure {
    private PressureStructure() {}

    static List<Offset> offsets(int frontX, int frontZ) {
        if (Math.abs(frontX) + Math.abs(frontZ) != 1) throw new IllegalArgumentException("Horizontal facing required");
        var result = new ArrayList<Offset>(7);
        for (int i = 1; i < 8; i++) {
            int right = i % 2;
            int back = i / 2 % 2;
            result.add(new Offset(frontZ * right - frontX * back, -(i / 4), -frontX * right - frontZ * back));
        }
        return List.copyOf(result);
    }

    record Offset(int x, int y, int z) {}
}
