package ic2.core.machine;

enum MachineKind {
    GENERATOR(4000, 0, 0),
    SOLAR_PANEL(0, 0, 0),
    MACERATOR(2, 400),
    ELECTRIC_FURNACE(3, 130),
    EXTRACTOR(2, 400),
    COMPRESSOR(2, 400),
    PUMP(200, 1, 120),
    BATBOX(40000, 0, 0);

    final int capacity;
    final int energyPerTick;
    final int recipeTicks;

    MachineKind(int energyPerTick, int recipeTicks) {
        this(energyPerTick * recipeTicks, energyPerTick, recipeTicks);
    }

    MachineKind(int capacity, int energyPerTick, int recipeTicks) {
        this.capacity = capacity;
        this.energyPerTick = energyPerTick;
        this.recipeTicks = recipeTicks;
    }
}
