package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

final class PowerPartEntity extends BlockEntity implements ic2.api.machine.MachineSnapshot.Access {
    private BlockPos owner;
    boolean ownedBy(BlockPos controller) { return controller.equals(owner); }
    PowerPartEntity(BlockPos pos, BlockState state) { super(PowerMachines.PART_ENTITY.get(), pos, state); }
    boolean canClaim(BlockPos pos) {
        if (owner == null || owner.equals(pos)) return true;
        if (!level.isLoaded(owner)) return false;
        return !(level.getBlockEntity(owner) instanceof PowerMachineEntity machine && machine.contains(worldPosition) && machine.shapeValid());
    }
    void claim(BlockPos pos) {
        if (!pos.equals(owner)) { owner = pos.immutable(); setChanged(); level.invalidateCapabilities(worldPosition); }
    }
    PowerMachineEntity controller() {
        if (level == null || isRemoved() || owner == null || !level.hasChunkAt(worldPosition)
            || level.getBlockEntity(worldPosition) != this || !level.isLoaded(owner)) return null;
        return level.getBlockEntity(owner) instanceof PowerMachineEntity machine && machine.contains(worldPosition) && machine.formed() ? machine : null;
    }
    @Override public java.util.Optional<ic2.api.machine.MachineSnapshot> resolveMachineInfo() {
        return java.util.Optional.ofNullable(controller()).map(PowerMachineEntity::machineInfo);
    }
    // Resolve at operation time so a cached capability cannot access a broken or reassigned structure.
    IItemHandler items(Direction side) {
        return new IItemHandler() {
            IItemHandler target() { var machine = controller(); return machine == null ? null : machine.automation(side); }
            @Override public int getSlots() { var t = target(); return t == null ? 0 : t.getSlots(); }
            @Override public ItemStack getStackInSlot(int slot) { var t = target(); return t == null ? ItemStack.EMPTY : t.getStackInSlot(slot); }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) { var t = target(); return t == null ? stack : t.insertItem(slot, stack, simulate); }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { var t = target(); return t == null ? ItemStack.EMPTY : t.extractItem(slot, amount, simulate); }
            @Override public int getSlotLimit(int slot) { var t = target(); return t == null ? 0 : t.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, ItemStack stack) { var t = target(); return t != null && t.isItemValid(slot, stack); }
        };
    }
    IFluidHandler fluids() {
        return new IFluidHandler() {
            IFluidHandler target() { var machine = controller(); return machine == null ? null : machine.fluidHandler(); }
            @Override public int getTanks() { var t = target(); return t == null ? 0 : t.getTanks(); }
            @Override public FluidStack getFluidInTank(int tank) { var t = target(); return t == null ? FluidStack.EMPTY : t.getFluidInTank(tank); }
            @Override public int getTankCapacity(int tank) { var t = target(); return t == null ? 0 : t.getTankCapacity(tank); }
            @Override public boolean isFluidValid(int tank, FluidStack stack) { var t = target(); return t != null && t.isFluidValid(tank, stack); }
            @Override public int fill(FluidStack stack, FluidAction action) { var t = target(); return t == null ? 0 : t.fill(stack, action); }
            @Override public FluidStack drain(FluidStack stack, FluidAction action) { var t = target(); return t == null ? FluidStack.EMPTY : t.drain(stack, action); }
            @Override public FluidStack drain(int amount, FluidAction action) { var t = target(); return t == null ? FluidStack.EMPTY : t.drain(amount, action); }
        };
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup); if (owner != null) tag.putLong("controller", owner.asLong());
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup); owner = tag.contains("controller") ? BlockPos.of(tag.getLong("controller")) : null;
    }
}
