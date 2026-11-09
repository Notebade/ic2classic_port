package ic2.core.machine;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

final class SteamReactorTanks implements IFluidHandler {
    static final int WATER_CAPACITY = 2_000;
    static final int STEAM_CAPACITY = 20_000;
    private final FluidTank water;
    private final FluidTank steam;

    SteamReactorTanks(Runnable changed) {
        water = new FluidTank(WATER_CAPACITY, stack -> stack.getFluid() == Fluids.WATER) {
            @Override
            protected void onContentsChanged() {
                changed.run();
            }
        };
        steam = new FluidTank(STEAM_CAPACITY, stack -> stack.getFluid() == SteamFluids.steam()) {
            @Override
            protected void onContentsChanged() {
                changed.run();
            }
        };
    }

    int refillVent(int amount) {
        return water.drain(amount, FluidAction.EXECUTE).getAmount();
    }

    boolean produce(int amount) {
        return amount > 0 && steam.getSpace() >= amount
            && steam.fill(new FluidStack(SteamFluids.steam(), amount), FluidAction.EXECUTE) == amount;
    }

    void push(Level level, BlockPos core, List<BlockPos> chambers) {
        pushFrom(level, core);
        for (BlockPos chamber : chambers) {
            pushFrom(level, chamber);
        }
    }

    private void pushFrom(Level level, BlockPos origin) {
        for (Direction direction : Direction.values()) {
            if (steam.isEmpty()) {
                return;
            }
            BlockPos target = origin.relative(direction);
            if (level.isLoaded(target)) {
                var sink = level.getCapability(Capabilities.FluidHandler.BLOCK, target, direction.getOpposite());
                if (sink != null && sink != this) {
                    steam.drain(sink.fill(steam.getFluid().copy(), FluidAction.EXECUTE), FluidAction.EXECUTE);
                }
            }
        }
    }

    CompoundTag save(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        tag.put("water", water.writeToNBT(registries, new CompoundTag()));
        tag.put("steam", steam.writeToNBT(registries, new CompoundTag()));
        return tag;
    }

    void load(HolderLookup.Provider registries, CompoundTag tag) {
        water.readFromNBT(registries, tag.getCompound("water"));
        steam.readFromNBT(registries, tag.getCompound("steam"));
    }

    @Override public int getTanks() { return 2; }
    @Override public FluidStack getFluidInTank(int tank) { return (tank == 0 ? water : steam).getFluid(); }
    @Override public int getTankCapacity(int tank) { return tank == 0 ? WATER_CAPACITY : STEAM_CAPACITY; }
    @Override public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0 && water.isFluidValid(stack); }
    @Override public int fill(FluidStack stack, FluidAction action) { return water.fill(stack, action); }
    @Override public FluidStack drain(FluidStack stack, FluidAction action) { return steam.drain(stack, action); }
    @Override public FluidStack drain(int amount, FluidAction action) { return steam.drain(amount, action); }
}
