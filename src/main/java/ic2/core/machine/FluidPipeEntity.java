package ic2.core.machine;

import ic2.api.machine.MachineSnapshot;
import java.util.ArrayDeque;
import java.util.HashSet;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Buffered routing visits loaded pipes once and never pushes fluid into another pipe buffer.
 */
/** Visits loaded pipe nodes once; endpoint handlers receive fluid without filling intermediate pipe buffers. */
final class FluidPipeEntity extends BlockEntity implements MachineSnapshot.Provider {
    private boolean routing;
    final FluidTank tank = new FluidTank(8000){

        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };

    FluidPipeEntity(BlockPos p, BlockState s) {
        super(NuclearInfrastructure.PIPE_ENTITY.get(), p, s);
    }

    void tick() {
        if (routing || tank.isEmpty()) return;
        routing = true;
        try {
            route(((FluidPipeBlock)getBlockState().getBlock()).pressure ? 1000 : 100);
        } finally {
            routing = false;
        }
    }

    private void route(int budget) {
        var queue = new ArrayDeque<BlockPos>();
        var visited = new HashSet<BlockPos>();
        queue.add(worldPosition);
        visited.add(worldPosition);
        var endpoints = new HashSet<Endpoint>();
        while (!queue.isEmpty() && budget > 0 && !tank.isEmpty()) {
            BlockPos pipe = queue.removeFirst();
            for (Direction side : Direction.values()) {
                BlockPos next = pipe.relative(side);
                if (!level.hasChunkAt(next)) continue;
                if (level.getBlockEntity(next) instanceof FluidPipeEntity) {
                    if (visited.size() < 256 && visited.add(next)) queue.add(next);
                    continue;
                }
                if (!endpoints.add(new Endpoint(next, side.getOpposite()))) continue;
                var target = level.getCapability(Capabilities.FluidHandler.BLOCK, next, side.getOpposite());
                if (target == null) continue;
                int offered = Math.min(budget, tank.getFluidAmount());
                int accepted = Math.clamp(target.fill(tank.getFluid().copyWithAmount(offered), FluidAction.EXECUTE), 0, offered);
                tank.drain(accepted, FluidAction.EXECUTE);
                budget -= accepted;
                if (budget == 0 || tank.isEmpty()) return;
            }
        }
    }
    private record Endpoint(BlockPos position, Direction side) {}

    @Override
    public MachineSnapshot machineInfo() {
        return MachineTelemetry.snapshot(this, 0, 0, MachineSnapshot.EnergyUnit.NONE, Map.of("fluid_mb", (long)tank.getFluidAmount(), "capacity_mb", 8000L, "transfer_mb", ((FluidPipeBlock)getBlockState().getBlock()).pressure ? 1000L : 100L));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup);
        tag.put("tank", tank.writeToNBT(lookup, new CompoundTag()));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        tank.readFromNBT(lookup, tag.getCompound("tank"));
    }
}
