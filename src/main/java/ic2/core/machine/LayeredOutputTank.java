package ic2.core.machine;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/** Output-only fluid layers sharing a single capacity. */
final class LayeredOutputTank implements IFluidHandler {
    private final List<FluidTank> layers = new ArrayList<>();
    private final Runnable changed;
    private final int capacity;
    LayeredOutputTank(int capacity, Runnable changed) { this.capacity = capacity; this.changed = changed; }
    int amount() { return layers.stream().mapToInt(FluidTank::getFluidAmount).sum(); }
    int room() { return capacity - amount(); }
    int produce(FluidStack stack, FluidAction action) {
        int accepted = Math.min(stack.getAmount(), room());
        if (stack.isEmpty() || accepted <= 0) return 0;
        if (!action.execute()) return accepted;
        for (var tank : layers) if (FluidStack.isSameFluidSameComponents(tank.getFluid(), stack)) {
            tank.fill(stack.copyWithAmount(accepted), FluidAction.EXECUTE); changed.run(); return accepted;
        }
        var layer = new FluidTank(capacity); layer.setFluid(stack.copyWithAmount(accepted)); layers.add(layer); changed.run(); return accepted;
    }
    @Override public int getTanks() { return layers.size() + 1; }
    @Override public FluidStack getFluidInTank(int tank) { return tank >= 0 && tank < layers.size() ? layers.get(tank).getFluid().copy() : FluidStack.EMPTY; }
    @Override public int getTankCapacity(int tank) { return capacity; }
    @Override public boolean isFluidValid(int tank, FluidStack stack) { return false; }
    @Override public int fill(FluidStack stack, FluidAction action) { return 0; }
    @Override public FluidStack drain(FluidStack stack, FluidAction action) {
        if (stack.isEmpty()) return FluidStack.EMPTY;
        for (var tank : layers) if (FluidStack.isSameFluidSameComponents(tank.getFluid(), stack)) return drainLayer(tank, stack.getAmount(), action);
        return FluidStack.EMPTY;
    }
    @Override public FluidStack drain(int amount, FluidAction action) {
        return amount <= 0 || layers.isEmpty() ? FluidStack.EMPTY : drainLayer(layers.getFirst(), amount, action);
    }
    private FluidStack drainLayer(FluidTank tank, int amount, FluidAction action) {
        var result = tank.drain(amount, action);
        if (action.execute() && !result.isEmpty()) { if (tank.isEmpty()) layers.remove(tank); changed.run(); }
        return result;
    }
    ListTag save(HolderLookup.Provider lookup) {
        var list = new ListTag(); for (var tank : layers) list.add(tank.writeToNBT(lookup, new CompoundTag())); return list;
    }
    void load(ListTag list, HolderLookup.Provider lookup) {
        layers.clear();
        for (Tag entry : list) {
            var tank = new FluidTank(capacity); tank.readFromNBT(lookup, (CompoundTag) entry);
            produce(tank.getFluid(), FluidAction.EXECUTE);
        }
    }
}
