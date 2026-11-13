package ic2.core.machine;

import ic2.core.item.RechargeableItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

final class BatterySlot extends RangedWrapper {
    private final boolean charging;

    BatterySlot(IItemHandlerModifiable inventory, int slot, boolean charging) {
        super(inventory, slot, slot + 1);
        this.charging = charging;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return super.isItemValid(slot, stack) && !completed(stack);
    }

    @Override
    public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
        return isItemValid(slot, stack) ? super.insertItem(slot, stack, simulate) : stack;
    }

    @Override
    public ItemStack extractItem(int slot, int amount, boolean simulate) {
        return completed(getStackInSlot(slot)) ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
    }

    private boolean completed(ItemStack stack) {
        return stack.getCount() == 1 && stack.getItem() instanceof ic2.api.item.ElectricItem cell
            && cell.charge(stack) == (charging ? cell.capacity() : 0);
    }
}
