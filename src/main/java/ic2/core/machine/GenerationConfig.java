package ic2.core.machine;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Server-owned balance settings; defaults match IC2 Classic.
 */
public final class GenerationConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    static final ModConfigSpec.IntValue NUCLEAR = BUILDER.defineInRange("nuclearGeneratorMultiplier", 20, 1, 1000);
    static final ModConfigSpec.DoubleValue SOLAR = BUILDER.defineInRange("solarTurbineMultiplier", 1.0, 0.1, 100.0);
    static final ModConfigSpec.IntValue SLAG = BUILDER.defineInRange("slagGenBase", 60, 1, 8000);
    static final ModConfigSpec.IntValue WIND = BUILDER.defineInRange("windmillOutput", 256, 1, 4000);
    static final ModConfigSpec.IntValue TUNNEL = BUILDER.defineInRange("steamTunnelBase", 512, 1, 10000);
    static final ModConfigSpec.IntValue STEAM = BUILDER.defineInRange("steamTurbineBase", 64, 1, 4000);
    static final ModConfigSpec.DoubleValue LIQUID = BUILDER.defineInRange("fluidGeneratorMultiplier", 1.0, 0.1, 10.0);
    static final ModConfigSpec.DoubleValue STEAM_REACTOR = BUILDER.defineInRange("steamReactorMultiplier", 2.0, 0.1, 100.0);
    static final ModConfigSpec.DoubleValue REACTOR_DAMAGE = BUILDER.defineInRange("reactorDamage", 45.0, 0.0, 1000.0);
    public static final ModConfigSpec SPEC = BUILDER.build();

    private GenerationConfig() {
    }
}
