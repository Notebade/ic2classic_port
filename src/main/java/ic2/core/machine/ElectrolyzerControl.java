package ic2.core.machine;

final class ElectrolyzerControl {
    static final int MAX_BUFFER = returnedEnergy(20000, 6);
    enum Mode { CHARGE, IDLE, DISCHARGE }

    private ElectrolyzerControl() {}

    static Mode mode(int energy, int capacity) {
        if (energy * 10L < capacity * 3L) return Mode.DISCHARGE;
        return energy * 10L >= capacity * 7L ? Mode.CHARGE : Mode.IDLE;
    }

    static int returnedEnergy(int recipeEnergy, int tier) {
        return recipeEnergy / 10 * (6 + tier);
    }
}
