package ic2.core.machine;

import ic2.core.IC2;
import ic2.core.item.RechargeableItem;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

final class GasifierEngine implements ChemicalEngine {
    private final boolean electric;
    private final OutputTank tank;
    private int remaining, fuel, maxFuel;
    private final ItemStackHandler inventory;
    GasifierEngine(boolean electric, Runnable changed, ItemStackHandler inventory) {
        this.electric = electric; this.inventory = inventory;
        tank = new OutputTank(electric ? 18000 : 9000, changed);
    }
    @Override public Tick tick(ServerLevel level, BlockPos pos, int energy, int clock, ItemStackHandler ignored) {
        if (electric) energy = ChemicalTransfer.discharge(energy, 500, 32, inventory);
        if (!electric && fuel == 0 && (remaining > 0 || canStart())) refuel();
        boolean powered = electric ? energy > 0 : fuel > 0;
        if (powered && remaining == 0 && canStart()) {
            remaining = electric ? 80 : 160; inventory.extractItem(1, 1, false);
        }
        var step = GasifierCycle.step(remaining, powered, electric);
        boolean room = tank.getSpace() >= step.gas() && (!step.complete() || charcoalRoom());
        boolean running = powered && remaining > 0 && room;
        if (running) {
            remaining = step.remaining(); tank.produce(step.gas());
            if (step.complete()) ChemicalTransfer.add(inventory, 2, new ItemStack(Items.CHARCOAL));
            if (electric) energy--;
        }
        if (!electric && fuel > 0) fuel--; // Lit solid fuel keeps burning while a machine is idle.
        if (clock % 10 == 0) {
            ChemicalTransfer.container(inventory, 3, 4, tank, true);
            ChemicalTransfer.push(level, pos, tank, 25);
        }
        return new Tick(energy, electric ? running : fuel > 0);
    }
    private boolean charcoalRoom() { return ChemicalTransfer.fits(inventory, 2, new ItemStack(Items.CHARCOAL)); }
    private boolean canStart() {
        return tank.getFluidAmount() <= (electric ? 17000 : 8000) && inventory.getStackInSlot(1).is(ItemTags.LOGS) && charcoalRoom();
    }
    private static int fuelTicks(ItemStack stack) { return stack.isEmpty() ? 0 : Math.max(0, stack.getBurnTime(RecipeType.SMELTING)); }
    private void refuel() {
        var stack = inventory.getStackInSlot(0); int ticks = fuelTicks(stack);
        if (ticks == 0) return;
        var residue = stack.getCraftingRemainingItem();
        if (stack.getCount() > 1 && !ChemicalTransfer.fits(inventory, 4, residue)) return;
        inventory.extractItem(0, 1, false);
        if (!residue.isEmpty()) {
            if (inventory.getStackInSlot(0).isEmpty()) inventory.setStackInSlot(0, residue);
            else ChemicalTransfer.add(inventory, 4, residue);
        }
        fuel = ticks; maxFuel = ticks;
    }
    @Override public boolean accepts(int slot, ItemStack stack) {
        return switch (slot) {
            case 0 -> electric ? RechargeableItem.supports(stack, 32, false) : fuelTicks(stack) > 0;
            case 1 -> stack.is(ItemTags.LOGS);
            case 3 -> electric ? stack.getCapability(net.neoforged.neoforge.capabilities.Capabilities.FluidHandler.ITEM) != null
                && FluidUtil.getFluidContained(stack).isEmpty() : stack.is(IC2.CELL_EMPTY.get());
            default -> false;
        };
    }
    @Override public IItemHandler automation(Direction side) {
        int[] slots = side == Direction.UP ? new int[]{1, 3} : side == Direction.DOWN ? new int[]{0, 2, 4} : new int[]{0, 1, 2, 3, 4};
        return ChemicalTransfer.view(inventory, slots, slot -> slot == 2 || slot == 4 || slot == 0
            && (electric ? ChemicalTransfer.discharged(inventory.getStackInSlot(0)) : fuelTicks(inventory.getStackInSlot(0)) == 0));
    }
    @Override public IFluidHandler fluids() { return tank; }
    @Override public int metric(int index) {
        return switch (index) { case 13 -> remaining == 0 ? 0 : (electric ? 80 : 160) - remaining;
            case 14 -> tank.getFluidAmount(); case 17 -> electric ? 80 : 160; case 18 -> fuel; default -> 0; };
    }
    @Override public void telemetry(Map<String, Long> values) {
        values.put("progress_units", (long) metric(13)); values.put("progress_target", (long) metric(17));
        values.put("gas_mb", (long) tank.getFluidAmount()); values.put("fuel_ticks", (long) fuel);
    }
    @Override public CompoundTag save(HolderLookup.Provider lookup) {
        var tag = new CompoundTag(); tank.writeToNBT(lookup, tag); tag.putInt("remaining", remaining);
        tag.putInt("fuel", fuel); tag.putInt("max_fuel", maxFuel); return tag;
    }
    @Override public void load(CompoundTag tag, HolderLookup.Provider lookup) {
        tank.readFromNBT(lookup, tag);
        if (!tank.isEmpty() && !tank.getFluid().is(GeneratorFluids.Fuel.WOODGAS.fluid.get())) tank.setFluid(FluidStack.EMPTY);
        remaining = Mth.clamp(tag.getInt("remaining"), 0, electric ? 80 : 160);
        fuel = Math.max(0, tag.getInt("fuel")); maxFuel = Math.max(fuel, tag.getInt("max_fuel"));
    }
    private static final class OutputTank extends FluidTank {
        OutputTank(int capacity, Runnable changed) {
            super(capacity, fluid -> fluid.is(GeneratorFluids.Fuel.WOODGAS.fluid.get())); this.changed = changed;
        }
        private final Runnable changed;
        @Override protected void onContentsChanged() { changed.run(); }
        @Override public int fill(FluidStack stack, FluidAction action) { return 0; }
        void produce(int amount) { super.fill(new FluidStack(GeneratorFluids.Fuel.WOODGAS.fluid.get(), amount), FluidAction.EXECUTE); }
    }
}
