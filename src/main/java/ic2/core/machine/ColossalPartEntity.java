package ic2.core.machine;

import ic2.api.machine.MachineSnapshot;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

final class ColossalPartEntity extends BlockEntity implements EnergySink, MachineSnapshot.Access {
    private BlockPos owner;
    ColossalPartEntity(BlockPos pos, BlockState state) { super(ColossalMachines.PART_ENTITY.get(), pos, state); }

    boolean canClaim(BlockPos controller) {
        if (owner == null || owner.equals(controller)) return true;
        if (!level.hasChunkAt(owner)) return false;
        return !(level.getBlockEntity(owner) instanceof ColossalEntity machine && machine.contains(worldPosition) && machine.geometryValid());
    }
    void claim(BlockPos pos) {
        if (!pos.equals(owner)) { owner = pos.immutable(); setChanged(); level.invalidateCapabilities(worldPosition); }
    }
    ColossalEntity controller() {
        if (level == null || isRemoved() || owner == null || !level.hasChunkAt(worldPosition) || level.getBlockEntity(worldPosition) != this || !level.hasChunkAt(owner)) return null;
        return level.getBlockEntity(owner) instanceof ColossalEntity machine && machine.contains(worldPosition) && machine.live() ? machine : null;
    }
    IItemHandler items(Direction side) { return GuardedCapabilities.items(() -> { var machine = controller(); return machine == null ? null : machine.automation(side); }); }
    IFluidHandler fluids() { return GuardedCapabilities.fluids(() -> { var machine = controller(); return machine == null ? null : machine.tanks; }); }
    @Override public boolean canReceiveEnergy(Direction side, int sourceStorageOutput, int packet) { var machine = controller(); return machine != null && machine.canReceiveEnergy(side, sourceStorageOutput, packet); }
    @Override public int receiveEnergy(int offered) { var machine = controller(); return machine == null ? 0 : machine.receiveEnergy(offered); }
    @Override public Optional<MachineSnapshot> resolveMachineInfo() { return Optional.ofNullable(controller()).map(ColossalEntity::machineInfo); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) { super.saveAdditional(tag, lookup); if (owner != null) tag.putLong("owner", owner.asLong()); }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) { super.loadAdditional(tag, lookup); owner = tag.contains("owner") ? BlockPos.of(tag.getLong("owner")) : null; }
}
