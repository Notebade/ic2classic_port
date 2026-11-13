package ic2.api.machine;

import net.minecraft.world.item.ItemStack;

/** Item contract for passive machine modifiers. Values apply once per item in a stack. */
public interface MachineUpgrade {
    default double speedMultiplier(ItemStack stack) { return 1; }
    default double energyDemandMultiplier(ItemStack stack) { return 1; }
    default int extraEnergyStorage(ItemStack stack) { return 0; }
    default double storageMultiplier(ItemStack stack) { return 1; }
    default int extraTier(ItemStack stack) { return 0; }
}
