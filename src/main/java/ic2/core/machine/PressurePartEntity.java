package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

final class PressurePartEntity extends BlockEntity implements EnergySink, ic2.api.machine.MachineSnapshot.Access {
    private BlockPos owner;

    PressurePartEntity(BlockPos pos, BlockState state) { super(AdvancedMachines.PRESSURE_PART_ENTITY.get(), pos, state); }

    boolean canClaim(BlockPos controller) {
        if (owner == null || owner.equals(controller)) return true;
        if (!level.isLoaded(owner)) return false;
        return !(level.getBlockEntity(owner) instanceof AdvancedMachineEntity other
            && other.pressureContains(worldPosition) && other.pressureShapeValid());
    }

    void claim(BlockPos controller) {
        if (!controller.equals(owner)) {
            owner = controller.immutable();
            setChanged();
            level.invalidateCapabilities(worldPosition);
        }
    }

    boolean ownedBy(BlockPos controller) { return controller.equals(owner); }

    AdvancedMachineEntity controller() {
        if (level == null || level.isClientSide || isRemoved() || !level.hasChunkAt(worldPosition) || level.getBlockEntity(worldPosition) != this) return null;
        return owner != null && level.isLoaded(owner) ? candidate(owner) : null;
    }

    private AdvancedMachineEntity candidate(BlockPos pos) {
        return level.getBlockEntity(pos) instanceof AdvancedMachineEntity machine
            && machine.pressureContains(worldPosition) && machine.pressureLive() ? machine : null;
    }
    @Override public java.util.Optional<ic2.api.machine.MachineSnapshot> resolveMachineInfo() {
        return java.util.Optional.ofNullable(controller()).map(AdvancedMachineEntity::machineInfo);
    }

    @Override public boolean canReceiveEnergy(Direction side, int sourceStorageOutput, int packet) {
        var controller = controller();
        return controller != null && controller.canReceiveEnergy(side, sourceStorageOutput, packet);
    }

    @Override public int receiveEnergy(int available) {
        var controller = controller();
        return controller == null ? 0 : controller.receiveEnergy(available);
    }

    IItemHandler automation(Direction side) {
        // Resolve on every operation: cached capabilities must not retain an invalid or reassigned controller.
        return new IItemHandler() {
            private IItemHandler target() {
                var master = controller();
                return master == null ? null : master.automation(side);
            }
            @Override public int getSlots() { var target = target(); return target == null ? 0 : target.getSlots(); }
            @Override public ItemStack getStackInSlot(int slot) {
                var target = target(); return target == null ? ItemStack.EMPTY : target.getStackInSlot(slot);
            }
            @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                var target = target(); return target == null ? stack : target.insertItem(slot, stack, simulate);
            }
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
                var target = target(); return target == null ? ItemStack.EMPTY : target.extractItem(slot, amount, simulate);
            }
            @Override public int getSlotLimit(int slot) { var target = target(); return target == null ? 0 : target.getSlotLimit(slot); }
            @Override public boolean isItemValid(int slot, ItemStack stack) {
                var target = target(); return target != null && target.isItemValid(slot, stack);
            }
        };
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (owner != null) tag.putLong("controller", owner.asLong());
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        owner = tag.contains("controller") ? BlockPos.of(tag.getLong("controller")) : null;
    }
}
