package ic2.core.machine;

enum MachineKind {
    GENERATOR(4000, 0, 0),
    GEOTHERMAL(FluidGeneratorBlock.GEOTHERMAL_OUTPUT, 0, 0),
    WIND_TURBINE(32, 0, 0),
    WAVE_GENERATOR(1000, 0, 0),
    WATER_MILL(2, WaterMillTier.BASE),
    LV_WATER_MILL(32, WaterMillTier.LV),
    MV_WATER_MILL(256, WaterMillTier.MV),
    HV_WATER_MILL(2048, WaterMillTier.HV),
    SOLAR_PANEL(0, 0, 0),
    MACERATOR(2, 400),
    ELECTRIC_FURNACE(3, 130),
    ELECTRIC_BLAST_FURNACE(5, 65),
    ELECTRIC_SMOKER(5, 65),
    SAWMILL(2, 400),
    ALLOY_SMELTER(16, 250),
    EXTRACTOR(2, 400),
    RARE_EARTH_EXTRACTOR(1120, 1, 35),
    ELECTROLYZER(20_000, 0, 0),
    COMPRESSOR(2, 400),
    PUMP(200, 1, 120),
    RECYCLER(1, 45),
    MASS_FABRICATOR(7_100_000, 0, 0),
    BATBOX(40_000, 0, 0, 32),
    MFE(600_000, 0, 0, 128),
    MFSU(10_000_000, 0, 0, 512),
    LV_TRANSFORMER(TransformerBlock.LV_HIGH_PACKET * 2, 0, 0),
    MV_TRANSFORMER(TransformerBlock.MV_HIGH_PACKET * 2, 0, 0),
    HV_TRANSFORMER(TransformerBlock.HV_HIGH_PACKET * 2, 0, 0),
    EV_TRANSFORMER(TransformerBlock.EV_HIGH_PACKET * 2, 0, 0);

    final int capacity;
    final int energyPerTick;
    final int recipeTicks;
    final int storageOutput;
    final WaterMillTier waterMillTier;

    MachineKind(int energyPerTick, int recipeTicks) {
        this(energyPerTick * recipeTicks, energyPerTick, recipeTicks);
    }

    MachineKind(int capacity, int energyPerTick, int recipeTicks) {
        this(capacity, energyPerTick, recipeTicks, 0);
    }

    MachineKind(int capacity, int energyPerTick, int recipeTicks, int storageOutput) {
        this(capacity, energyPerTick, recipeTicks, storageOutput, null);
    }

    MachineKind(int capacity, WaterMillTier waterMillTier) {
        this(capacity, 0, 0, 0, waterMillTier);
    }

    private MachineKind(int capacity, int energyPerTick, int recipeTicks, int storageOutput, WaterMillTier waterMillTier) {
        this.capacity = capacity;
        this.energyPerTick = energyPerTick;
        this.recipeTicks = recipeTicks;
        this.storageOutput = storageOutput;
        this.waterMillTier = waterMillTier;
    }

    boolean isStorage() {
        return storageOutput > 0;
    }

    int cookingTicks(int vanillaTicks) {
        return this == ELECTRIC_FURNACE ? recipeTicks
            : Math.max(1, (int) Math.round(vanillaTicks * (recipeTicks / 100.0)));
    }
}
