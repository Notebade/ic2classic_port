package ic2.core.machine;

final class WhiskyCycle {
    static final int STAGE_TICKS = 1_209_600;
    record Step(int age, int stage) {}
    static Step step(int age, int stage, boolean ageing) {
        if (!ageing || stage >= 50) return new Step(age, stage);
        return age + 1 >= STAGE_TICKS ? new Step(0, stage + 1) : new Step(age + 1, stage);
    }
    static int grade(int stage) { return stage >= 50 ? 5 : stage >= 25 ? 4 : stage >= 15 ? 3 : stage >= 12 ? 2 : stage >= 10 ? 1 : 0; }
    static int evaporated(int water, int stage, int grist) { return (int) ((int) (water * (1 - stage / 100.0)) * (grist / 16.0)); }
    static int alcohol(int year) { return switch (year) { case 1 -> 10000; case 2 -> 12000; case 3 -> 15000; case 4 -> 25000; case 5 -> 50000; default -> 0; }; }
}
