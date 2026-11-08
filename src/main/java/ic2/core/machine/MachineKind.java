package ic2.core.machine;

enum MachineKind {
    GENERATOR(4000, 0, 0),
    GEOTHERMAL(GeothermalGeneratorBlock.OUTPUT, 0, 0),
    SOLAR_PANEL(0, 0, 0),
    MACERATOR(2, 400),
    ELECTRIC_FURNACE(3, 130),
    ALLOY_SMELTER(16, 250),
    EXTRACTOR(2, 400),
    RARE_EARTH_EXTRACTOR(1120, 1, 35),
    COMPRESSOR(2, 400),
    PUMP(200, 1, 120),
    RECYCLER(1, 45),
    MASS_FABRICATOR(7_100_000, 0, 0),
    BATBOX(40_000, 0, 0, 32),
    MFE(600_000, 0, 0, 128),
    MFSU(10_000_000, 0, 0, 512),
    LV_TRANSFORMER(TransformerBlock.LV_HIGH_PACKET * 2, 0, 0),
    MV_TRANSFORMER(TransformerBlock.MV_HIGH_PACKET * 2, 0, 0);

    final int capacity;
    final int energyPerTick;
    final int recipeTicks;
    final int storageOutput;

    MachineKind(int energyPerTick, int recipeTicks) {
        this(energyPerTick * recipeTicks, energyPerTick, recipeTicks);
    }

    MachineKind(int capacity, int energyPerTick, int recipeTicks) {
        this(capacity, energyPerTick, recipeTicks, 0);
    }

    MachineKind(int capacity, int energyPerTick, int recipeTicks, int storageOutput) {
        this.capacity = capacity;
        this.energyPerTick = energyPerTick;
        this.recipeTicks = recipeTicks;
        this.storageOutput = storageOutput;
    }

    boolean isStorage() {
        return storageOutput > 0;
    }
}
