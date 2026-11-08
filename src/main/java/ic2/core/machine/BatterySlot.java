package ic2.core.machine;

import ic2.core.item.ReBatteryItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

final class BatterySlot extends RangedWrapper {
    private final int completedCharge;

    BatterySlot(IItemHandlerModifiable inventory, int slot, boolean charging) {
        super(inventory, slot, slot + 1);
        completedCharge = charging ? ReBatteryItem.CAPACITY : 0;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return super.isItemValid(slot, stack) && ReBatteryItem.charge(stack) != completedCharge;
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        ItemStack battery = getStackInSlot(slot);
        return ReBatteryItem.isBattery(battery) && ReBatteryItem.charge(battery) == completedCharge
            ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }
}
