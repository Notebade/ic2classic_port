package ic2.core.equipment;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EquipmentConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    static final ModConfigSpec.BooleanValue ORIGINAL_QUANTUM = BUILDER.define("originalQuantumBehavior", false);
    public static final ModConfigSpec SPEC = BUILDER.build();
    private EquipmentConfig() {}
}
