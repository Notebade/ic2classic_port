package ic2.core.machine;

/** Original storage redstone modes; thresholds use whole EU packets. */
final class StorageControl {
    static final int MODE_COUNT = 8;

    private StorageControl() {}

    static boolean signal(int mode, int energy, int capacity, int packet) {
        return switch (mode) {
            case 1 -> energy >= capacity;
            case 2 -> energy > packet && energy < capacity;
            case 3 -> energy >= capacity / 2;
            case 4 -> energy <= capacity / 2;
            case 5 -> energy < packet;
            default -> false;
        };
    }

    static boolean allowsOutput(int mode, int energy, int capacity, int packet, boolean powered) {
        return energy >= packet && (!powered || mode != 6 && (mode != 7 || energy >= capacity));
    }

    static int comparator(int energy, int capacity) {
        return energy == 0 ? 0 : 1 + (int) (14L * energy / capacity);
    }
}
