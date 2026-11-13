package ic2.api.machine;

import net.minecraft.world.item.ItemStack;

/** Item contract for passive machine modifiers. Values apply once per item in a stack. */
public interface MachineUpgrade {
    enum Category { MACHINE, RECIPE, TRANSPORT, REDSTONE, AUDIO, PROCESSING, CUSTOM }
    default Category category(ItemStack stack) { return Category.MACHINE; }
    default double speedMultiplier(ItemStack stack) { return 1; }
    default double energyDemandMultiplier(ItemStack stack) { return 1; }
    default int extraEnergyStorage(ItemStack stack) { return 0; }
    default double storageMultiplier(ItemStack stack) { return 1; }
    default int extraTier(ItemStack stack) { return 0; }
    default boolean invertsRedstone(ItemStack stack) { return false; }
    default boolean togglesRedstoneSensitivity(ItemStack stack) { return false; }
    default double soundMultiplier(ItemStack stack) { return 1; }
    /** Return true if the upgrade's own state changed and needs saving. */
    default boolean tickUpgrade(ItemStack stack, UpgradeContext context) { return false; }
    /** Completed operation; result is a detached copy, or EMPTY for partial/chance-failed output. */
    default void recipeFinished(ItemStack stack, UpgradeContext context, ItemStack result, float experience) {}
}
