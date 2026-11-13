package ic2.core.machine;

import java.util.function.Supplier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/** Resolve the owner on every operation, including calls through previously cached capabilities. */
final class GuardedCapabilities {
    private GuardedCapabilities() {}
    static IItemHandler items(Supplier<IItemHandler> target) {
        return new IItemHandler() {
            @Override public int getSlots() { var value = target.get(); return value == null ? 0 : value.getSlots(); }
            @Override public ItemStack getStackInSlot(int slot) { var value = target.get(); return value == null ? ItemStack.EMPTY : value.getStackInSlot(slot); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { var value = target.get(); return value == null ? stack : value.insertItem(slot, stack, simulate); }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { var value = target.get(); return value == null ? ItemStack.EMPTY : value.extractItem(slot, amount, simulate); }
            @Override public int getSlotLimit(int slot) { var value = target.get(); return value == null ? 0 : value.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, ItemStack stack) { var value = target.get(); return value != null && value.isItemValid(slot, stack); }
        };
    }
    static IFluidHandler fluids(Supplier<IFluidHandler> target) {
        return new IFluidHandler() {
            @Override public int getTanks() { var value = target.get(); return value == null ? 0 : value.getTanks(); }
            @Override public FluidStack getFluidInTank(int tank) { var value = target.get(); return value == null ? FluidStack.EMPTY : value.getFluidInTank(tank); }
            @Override public int getTankCapacity(int tank) { var value = target.get(); return value == null ? 0 : value.getTankCapacity(tank); }
            @Override public boolean isFluidValid(int tank, FluidStack stack) { var value = target.get(); return value != null && value.isFluidValid(tank, stack); }
            @Override public int fill(FluidStack stack, FluidAction action) { var value = target.get(); return value == null ? 0 : value.fill(stack, action); }
            @Override public FluidStack drain(FluidStack stack, FluidAction action) { var value = target.get(); return value == null ? FluidStack.EMPTY : value.drain(stack, action); }
            @Override public FluidStack drain(int amount, FluidAction action) { var value = target.get(); return value == null ? FluidStack.EMPTY : value.drain(amount, action); }
        };
    }
}
