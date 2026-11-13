package ic2.core.machine;

final class FusionCycle {
    record Step(State next, int damage) {}
    record State(int heat, int material, int lava, double remainder) {
        Step step(double averageHeat, int rods, int slots, double rate) {
            if (heat < 48000 && averageHeat > 2) {
                return new Step(new State((int) Math.min(48000, heat + Math.min(5, averageHeat - 2) * slots * 10),
                    material, lava, remainder), rods / Math.max(1, slots));
            }
            if (heat != 48000 || material <= 0 || lava >= 522000 || rods == 0) return new Step(this, 0);
            double production = Math.max(0, lava >= 10000 ? averageHeat : Math.min(5, averageHeat - 2));
            double total = remainder < 522000 - lava ? remainder + production * rods * rate * 10 / 1.5 : remainder;
            int added = Math.min(Math.min(material, (int) total), 522000 - lava);
            int nextLava = lava + added;
            boolean bootstrap = added > 0 && nextLava < 10000;
            int nextHeat = bootstrap ? (int) (heat - (1 - nextLava / 10000.0) * 24000) : heat;
            return new Step(new State(nextHeat, material - added, nextLava, total - added),
                bootstrap ? rods / Math.max(1, slots) : 0);
        }
    }
}
