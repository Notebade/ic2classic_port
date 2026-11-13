package ic2.api.item;

import net.minecraft.world.item.ItemStack;

/** Opt-in battery module. A positive armor transfer rate admits this stack to a battery slot. */
public interface ArmorBattery extends ElectricItem {
    default int armorTransferRate(ItemStack stack) { return transferRate(stack); }
}
