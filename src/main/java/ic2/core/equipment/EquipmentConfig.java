package ic2.core.equipment;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class EquipmentConfig {
    private static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();
    static final ModConfigSpec.BooleanValue ORIGINAL_QUANTUM = BUILDER.define("originalQuantumBehavior", false);
    static final ModConfigSpec.BooleanValue SPECIAL_ELECTRIC = BUILDER.define("enableSpecialElectricArmor", true);
    static final ModConfigSpec.DoubleValue ARMOR_ABSORPTION = BUILDER.defineInRange("electricSuitAbsorptionScale", 1.0, .01, 500);
    static final ModConfigSpec.DoubleValue ARMOR_ENERGY_COST = BUILDER.defineInRange("electricSuitEnergyCostModifier", 1.0, .01, 500);
    public static final ModConfigSpec SPEC = BUILDER.build();
    private EquipmentConfig() {}
}
