package ic2.core.machine;

import ic2.api.machine.MachineSnapshot;

import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Initial single-fluid tank support for the boiler chain. */
final class PowerTankEntity extends BlockEntity implements MachineSnapshot.Provider {
    @Override public MachineSnapshot machineInfo() {
        return MachineTelemetry.snapshot(this, 0, 0, MachineSnapshot.EnergyUnit.NONE,
            java.util.Map.of("fluid_mb", (long) tank.getFluidAmount(), "fluid_capacity_mb", (long) tank.getCapacity()));
    }
    private UUID owner;
    final FluidTank tank = new FluidTank(128000) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    PowerTankEntity(BlockPos pos, BlockState state) { super(PowerMachines.TANK_ENTITY.get(), pos, state); }
    void setOwner(Player player) { if (getBlockState().is(PowerMachines.PERSONAL_TANK.get())) { owner = player.getUUID(); setChanged(); } }
    boolean allowed(Player player) { return owner == null || owner.equals(player.getUUID()) || player.hasPermissions(2); }
    void status(Player player) {
        if (!allowed(player)) { player.displayClientMessage(Component.translatable("gui.ic2.power_locked"), true); return; }
        player.displayClientMessage(Component.translatable("gui.ic2.power_tank", tank.getFluid().getHoverName(), tank.getFluidAmount(), tank.getCapacity()), true);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup); tank.writeToNBT(lookup, tag); if (owner != null) tag.putUUID("owner", owner);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup); tank.readFromNBT(lookup, tag); owner = tag.hasUUID("owner") ? tag.getUUID("owner") : null;
    }
}
