package ic2.core.machine;

import java.util.HashSet;

public final class PressureStructureTest {
    public static void main(String[] args) {
        for (int[] facing : new int[][] {{0, -1}, {1, 0}, {0, 1}, {-1, 0}}) {
            var parts = PressureStructure.offsets(facing[0], facing[1]);
            check(parts.size() == 7 && new HashSet<>(parts).size() == 7, "seven distinct casing blocks");
            check(!parts.contains(new PressureStructure.Offset(0, 0, 0)), "controller is not a casing");
            check(parts.stream().filter(p -> p.y() == 0).count() == 3, "three casings on the controller's layer");
            check(parts.stream().filter(p -> p.y() == -1).count() == 4, "four casings below the controller");
            check(parts.stream().allMatch(p -> Math.abs(p.x()) <= 1 && Math.abs(p.z()) <= 1), "all parts fit a two-block cube");
        }
        var north = PressureStructure.offsets(0, -1);
        check(north.getFirst().equals(new PressureStructure.Offset(-1, 0, 0)), "original builder's right is relative to the inward facing");
        check(north.contains(new PressureStructure.Offset(-1, -1, 1)), "back-bottom corner");
        var layout = new ProcessingLayout(2, 1);
        check(!layout.supportsSplit() && layout.processingPasses() == 1, "alloy ingredients form one operation");
        check(layout.firstOutput(1, true) == 3 && layout.lastOutput(1, true) == 3, "one shared output");
        System.out.println("Pressure structure: all checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
