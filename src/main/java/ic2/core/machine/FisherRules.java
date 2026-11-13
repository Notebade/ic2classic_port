package ic2.core.machine;

final class FisherRules {
    private FisherRules() {}
    static boolean validDepth(int depth) { return depth >= 5 && depth <= 10; }
    static int nextProgress(int progress, int energy) { return energy >= 150 ? Math.min(300, progress + 1) : Math.max(0, progress - 1); }
    static int luck(int depth) { return depth - 4; }
}
