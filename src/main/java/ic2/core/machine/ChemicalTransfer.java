package ic2.core.machine;

import ic2.core.item.RechargeableItem;
import java.util.function.IntPredicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

final class ChemicalTransfer {
    private ChemicalTransfer() {}
    static boolean fits(ItemStackHandler inventory, int slot, ItemStack stack) {
        var stored = inventory.getStackInSlot(slot);
        return stack.isEmpty() || stored.isEmpty() && stack.getCount() <= stack.getMaxStackSize()
            || ItemStack.isSameItemSameComponents(stored, stack) && stored.getCount() + stack.getCount() <= stored.getMaxStackSize();
    }
    static void add(ItemStackHandler inventory, int slot, ItemStack stack) {
        if (stack.isEmpty()) return;
        var old = inventory.getStackInSlot(slot);
        inventory.setStackInSlot(slot, old.isEmpty() ? stack.copy() : old.copyWithCount(old.getCount() + stack.getCount()));
    }
    static boolean container(ItemStackHandler inventory, int input, int output, IFluidHandler tank, boolean filling) {
        var stack = inventory.getStackInSlot(input);
        if (stack.isEmpty()) return false;
        var preview = filling ? FluidUtil.tryFillContainer(stack, tank, Integer.MAX_VALUE, null, false)
            : FluidUtil.tryEmptyContainer(stack, tank, Integer.MAX_VALUE, null, false);
        if (!preview.isSuccess() || !fits(inventory, output, preview.getResult())) return false;
        var result = filling ? FluidUtil.tryFillContainer(stack, tank, Integer.MAX_VALUE, null, true)
            : FluidUtil.tryEmptyContainer(stack, tank, Integer.MAX_VALUE, null, true);
        if (!result.isSuccess()) return false;
        inventory.extractItem(input, 1, false); add(inventory, output, result.getResult()); return true;
    }
    static int discharge(int energy, int capacity, int packet, ItemStackHandler inventory) {
        var stack = inventory.getStackInSlot(0);
        if (!RechargeableItem.supports(stack, packet, false)) return energy;
        var cell = (ic2.api.item.ElectricItem) stack.getItem();
        int moved = Math.min(Math.min(cell.transferRate(), cell.charge(stack)), capacity - energy);
        if (moved > 0) { cell.setCharge(stack, cell.charge(stack) - moved); inventory.setStackInSlot(0, stack); }
        return energy + moved;
    }
    static IItemHandler view(ItemStackHandler inventory, int[] slots, IntPredicate extract) {
        return new IItemHandler() {
            @Override public int getSlots() { return slots.length; }
            @Override public ItemStack getStackInSlot(int slot) { return inventory.getStackInSlot(slots[slot]); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { return inventory.insertItem(slots[slot], stack, simulate); }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return extract.test(slots[slot]) ? inventory.extractItem(slots[slot], amount, simulate) : ItemStack.EMPTY;
            }
            @Override public int getSlotLimit(int slot) { return inventory.getSlotLimit(slots[slot]); }
            @Override public boolean isItemValid(int slot, ItemStack stack) { return inventory.isItemValid(slots[slot], stack); }
        };
    }
    static boolean discharged(ItemStack stack) { return stack.getItem() instanceof ic2.api.item.ElectricItem item && item.charge(stack) == 0; }
    static void push(ServerLevel level, BlockPos pos, IFluidHandler source, int limit) {
        int remaining = limit;
        for (var side : Direction.values()) {
            var next = pos.relative(side);
            if (remaining == 0 || !level.hasChunkAt(next)) continue;
            var target = level.getCapability(Capabilities.FluidHandler.BLOCK, next, side.getOpposite());
            if (target == null || target == source) continue;
            var fluid = source.drain(remaining, IFluidHandler.FluidAction.SIMULATE);
            if (fluid.isEmpty()) return;
            int filled = target.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
            remaining -= source.drain(filled, IFluidHandler.FluidAction.EXECUTE).getAmount();
        }
    }
}
