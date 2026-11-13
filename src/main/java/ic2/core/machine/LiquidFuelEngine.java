package ic2.core.machine;

import ic2.core.item.RechargeableItem;
import java.util.Map;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

final class LiquidFuelEngine {
    private LiquidFuelCycle.State state = new LiquidFuelCycle.State(0, 0);
    private int maxFuel;
    private boolean running;
    final FluidTank tank;
    private final IItemHandler all, sides;
    LiquidFuelEngine(Runnable changed, ItemStackHandler inventory) {
        tank = new FluidTank(16000, fluid -> GeneratorFluids.fuel(fluid.getFluid()) != null) {
            @Override protected void onContentsChanged() { changed.run(); }
        };
        var input = new RangedWrapper(inventory, 1, 2) {
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        };
        var output = new RangedWrapper(inventory, 2, 3);
        sides = new CombinedInvWrapper(input, output);
        all = new CombinedInvWrapper(new BatterySlot(inventory, 0, true), input, output);
    }
    IItemHandler automation(Direction side) { return side == null || side == Direction.DOWN ? all : sides; }
    static boolean accepts(int slot, ItemStack stack) {
        if (slot == 0) return RechargeableItem.supports(stack, 128);
        return slot == 1 && FluidUtil.getFluidContained(stack).filter(fluid -> GeneratorFluids.fuel(fluid.getFluid()) != null).isPresent();
    }
    int tick(int energy, ItemStackHandler inventory) {
        if (state.remaining() == 0 && !tank.isEmpty()) {
            var profile = GeneratorFluids.fuel(tank.getFluid().getFluid());
            int amount = Math.min(1000, tank.getFluidAmount());
            int production = profile == null ? 0 : Math.max(1, (int) (profile.output * GenerationConfig.LIQUID.get()));
            if (profile != null && energy <= 10000 - production) {
                int ticks = LiquidFuelCycle.fuelTicks(profile.ticks, amount);
                if (ticks > 0) { state = new LiquidFuelCycle.State(ticks, production); maxFuel = ticks; tank.drain(amount, FluidAction.EXECUTE); }
            }
        }
        var step = state.step(energy);
        running = step.energy() > energy; state = step.next(); energy = step.energy();
        fillContainer(inventory);
        return charge(energy, inventory.getStackInSlot(0));
    }
    private void fillContainer(ItemStackHandler inventory) {
        if (tank.getSpace() < 1000) return;
        ItemStack input = inventory.getStackInSlot(1), output = inventory.getStackInSlot(2);
        if (input.isEmpty()) return;
        var preview = FluidUtil.tryEmptyContainer(input, tank, Integer.MAX_VALUE, null, false);
        if (!preview.isSuccess()) return;
        ItemStack empty = preview.getResult();
        if (!empty.isEmpty() && !output.isEmpty() && (!ItemStack.isSameItemSameComponents(output, empty)
                || output.getCount() + empty.getCount() > output.getMaxStackSize())) return;
        var result = FluidUtil.tryEmptyContainer(input, tank, Integer.MAX_VALUE, null, true);
        if (!result.isSuccess()) return;
        inventory.extractItem(1, 1, false);
        if (!result.getResult().isEmpty()) inventory.setStackInSlot(2,
            output.isEmpty() ? result.getResult() : output.copyWithCount(output.getCount() + result.getResult().getCount()));
    }
    private static int charge(int energy, ItemStack stack) {
        if (!RechargeableItem.supports(stack, 128)) return energy;
        var cell = (ic2.api.item.ElectricItem) stack.getItem();
        int moved = Math.min(energy, Math.min(cell.transferRate(stack), cell.capacity(stack) - cell.charge(stack)));
        if (moved > 0) cell.setCharge(stack, cell.charge(stack) + moved);
        return energy - moved;
    }
    boolean running() { return running; }
    int remaining() { return state.remaining(); }
    int packet() { return LiquidFuelCycle.packet(state.production()); }
    int output() { return state.production(); }
    void telemetry(Map<String, Long> values) {
        values.put("fuel_mb", (long) tank.getFluidAmount()); values.put("fuel_ticks", (long) state.remaining());
        values.put("fuel_max_ticks", (long) maxFuel); values.put("output_eu_hundredths", running ? state.production() * 100L : 0L);
    }
    CompoundTag save(HolderLookup.Provider lookup) {
        var tag = new CompoundTag(); tag.put("tank", tank.writeToNBT(lookup, new CompoundTag()));
        tag.putInt("remaining", state.remaining()); tag.putInt("production", state.production()); tag.putInt("max_fuel", maxFuel); return tag;
    }
    void load(CompoundTag tag, HolderLookup.Provider lookup) {
        tank.readFromNBT(lookup, tag.getCompound("tank"));
        if (!tank.isEmpty() && GeneratorFluids.fuel(tank.getFluid().getFluid()) == null) tank.setFluid(net.neoforged.neoforge.fluids.FluidStack.EMPTY);
        int production = tag.getInt("production");
        if (production < 1 || production > 1280) production = 0;
        maxFuel = Mth.clamp(tag.getInt("max_fuel"), 0, 24000);
        state = new LiquidFuelCycle.State(production == 0 ? 0 : Mth.clamp(tag.getInt("remaining"), 0, maxFuel), production);
    }
}
