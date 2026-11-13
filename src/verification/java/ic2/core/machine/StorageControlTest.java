package ic2.core.machine;

/** Run with java -ea after compiling this class with StorageControl. */
public final class StorageControlTest {
    public static void main(String[] args) {
        int capacity = 1_000_000_000;
        int packet = 32768;
        check(!StorageControl.signal(0, capacity, capacity, packet), "default emits no redstone");
        check(!StorageControl.signal(1, capacity - 1, capacity, packet), "full threshold excludes partial charge");
        check(StorageControl.signal(1, capacity, capacity, packet), "full threshold includes capacity");
        check(!StorageControl.signal(2, packet, capacity, packet), "partial excludes one packet");
        check(StorageControl.signal(2, packet + 1, capacity, packet), "partial starts above one packet");
        check(!StorageControl.signal(2, capacity, capacity, packet), "partial excludes full");
        check(StorageControl.signal(3, capacity / 2, capacity, packet), "half-full includes midpoint");
        check(!StorageControl.signal(3, capacity / 2 - 1, capacity, packet), "half-full excludes below midpoint");
        check(StorageControl.signal(4, capacity / 2, capacity, packet), "half-empty includes midpoint");
        check(!StorageControl.signal(4, capacity / 2 + 1, capacity, packet), "half-empty excludes above midpoint");
        check(StorageControl.signal(5, packet - 1, capacity, packet), "empty means less than a packet");
        check(!StorageControl.signal(5, packet, capacity, packet), "one packet is not empty");
        check(!StorageControl.allowsOutput(0, packet - 1, capacity, packet, false), "never emit partial packets");
        check(StorageControl.allowsOutput(0, packet, capacity, packet, true), "default ignores redstone input");
        check(!StorageControl.allowsOutput(6, capacity, capacity, packet, true), "powered mode six blocks output");
        check(StorageControl.allowsOutput(6, packet, capacity, packet, false), "unpowered mode six emits");
        check(!StorageControl.allowsOutput(7, capacity - 1, capacity, packet, true), "powered mode seven waits for full");
        check(StorageControl.allowsOutput(7, capacity, capacity, packet, true), "powered mode seven emits when full");
        check(StorageControl.allowsOutput(7, packet, capacity, packet, false), "unpowered mode seven emits partial storage");
        for (int tierCapacity : new int[] {40_000, 600_000, 10_000_000, 75_000_000, 500_000_000, capacity}) {
            check(StorageControl.comparator(0, tierCapacity) == 0, "empty comparator");
            check(StorageControl.comparator(1, tierCapacity) == 1, "nonempty comparator");
            check(StorageControl.comparator(tierCapacity, tierCapacity) == 15, "full comparator must not overflow");
            check(StorageControl.comparator(tierCapacity / 2, tierCapacity) == 8, "half comparator must not overflow");
        }
        System.out.println("Storage control: all boundary checks passed");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
