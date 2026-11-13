package ic2.core.machine;

import ic2.core.item.RechargeableItem;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

final class RefineryEngine implements ChemicalEngine, IFluidHandler {
    private final FluidTank[] input = new FluidTank[2];
    private final LayeredOutputTank output;
    private final ItemStackHandler inventory;
    private int progress, lastTank;
    private String recipeId = "";
    private ItemStack trackedInput = ItemStack.EMPTY;
    private RefineryRecipes.Plan plan;
    RefineryEngine(Runnable changed, ItemStackHandler inventory) {
        this.inventory = inventory; output = new LayeredOutputTank(64000, changed);
        for (int slot = 0; slot < 2; slot++) input[slot] = new FluidTank(16000, stack -> RefineryRecipes.validFluid(stack.getFluid())) {
            @Override protected void onContentsChanged() { changed.run(); }
        };
    }
    @Override public Tick tick(ServerLevel level, BlockPos pos, int energy, int clock, ItemStackHandler ignored) {
        energy = ChemicalTransfer.discharge(energy, 5624, 128, inventory);
        plan = RefineryRecipes.find(inventory.getStackInSlot(1), input[0].getFluid(), input[1].getFluid());
        if (plan != null && (!plan.recipe().id().equals(recipeId) || plan.firstTank() != lastTank
                || !ItemStack.isSameItemSameComponents(trackedInput, inventory.getStackInSlot(1)))) progress = 0;
        if (plan != null) { recipeId = plan.recipe().id(); lastTank = plan.firstTank(); trackedInput = inventory.getStackInSlot(1).copyWithCount(1); }
        boolean room = plan != null && output.room() >= plan.fluidOutput().getAmount() && fits(plan.itemOutput());
        var step = RefiningCycle.step(progress, plan == null ? 250 : plan.recipe().ticks(), 20, energy, plan != null, room);
        progress = step.progress();
        if (step.complete()) finish(plan);
        return new Tick(step.energy(), step.active());
    }
    private boolean fits(ItemStack result) {
        if (result.isEmpty()) return true;
        int room = 0;
        for (int slot = 2; slot < 5; slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty()) room += result.getMaxStackSize();
            else if (ItemStack.isSameItemSameComponents(stack, result)) room += result.getMaxStackSize() - stack.getCount();
        }
        return room >= result.getCount();
    }
    private void finish(RefineryRecipes.Plan batch) {
        var recipe = batch.recipe();
        input[batch.firstTank()].drain(recipe.firstAmount(), FluidAction.EXECUTE);
        input[batch.secondTank()].drain(recipe.secondAmount(), FluidAction.EXECUTE);
        inventory.extractItem(1, recipe.count(), false);
        output.produce(batch.fluidOutput(), FluidAction.EXECUTE);
        ItemStack result = batch.itemOutput().copy();
        for (int slot = 2; slot < 5 && !result.isEmpty(); slot++) {
            var stored = inventory.getStackInSlot(slot);
            if (!stored.isEmpty() && !ItemStack.isSameItemSameComponents(stored, result)) continue;
            int amount = Math.min(result.getCount(), result.getMaxStackSize() - stored.getCount());
            if (amount > 0) { ChemicalTransfer.add(inventory, slot, result.copyWithCount(amount)); result.shrink(amount); }
        }
    }
    @Override public boolean accepts(int slot, ItemStack stack) {
        return slot == 0 ? RechargeableItem.supports(stack, 128, false) : slot == 1 && RefineryRecipes.accepts(stack);
    }
    @Override public IItemHandler automation(Direction side) {
        int[] slots = side == Direction.UP ? new int[]{1} : side == Direction.DOWN ? new int[]{0, 2, 3, 4} : new int[]{0, 1, 2, 3, 4};
        return ChemicalTransfer.view(inventory, slots, slot -> slot >= 2 || slot == 0 && ChemicalTransfer.discharged(inventory.getStackInSlot(0)));
    }
    @Override public IFluidHandler fluids() { return this; }
    @Override public int getTanks() { return 2 + output.getTanks(); }
    @Override public FluidStack getFluidInTank(int tank) { return tank < 2 ? input[tank].getFluid().copy() : output.getFluidInTank(tank - 2); }
    @Override public int getTankCapacity(int tank) { return tank < 2 ? 16000 : 64000; }
    @Override public boolean isFluidValid(int tank, FluidStack stack) { return tank < 2 && RefineryRecipes.validFluid(stack.getFluid()); }
    @Override public int fill(FluidStack resource, FluidAction action) {
        if (resource.isEmpty() || !RefineryRecipes.validFluid(resource.getFluid())) return 0;
        for (var tank : input) if (!tank.isEmpty() && FluidStack.isSameFluidSameComponents(tank.getFluid(), resource)) return tank.fill(resource, action);
        for (var tank : input) if (tank.isEmpty()) return tank.fill(resource, action);
        return 0;
    }
    @Override public FluidStack drain(FluidStack resource, FluidAction action) { return output.drain(resource, action); }
    @Override public FluidStack drain(int amount, FluidAction action) { return output.drain(amount, action); }
    @Override public boolean button(int button) {
        if (button == 6 || button == 7) {
            var tank = input[button - 6]; int moved = output.produce(tank.getFluid(), FluidAction.EXECUTE);
            if (moved == 0) return false;
            tank.drain(moved, FluidAction.EXECUTE);
        } else if (button == 8) {
            var fluid = output.drain(Integer.MAX_VALUE, FluidAction.SIMULATE);
            int moved = fill(fluid, FluidAction.EXECUTE);
            if (moved == 0) return false;
            output.drain(fluid.copyWithAmount(moved), FluidAction.EXECUTE);
        } else return false;
        progress = 0; recipeId = ""; return true;
    }
    @Override public int metric(int index) {
        return switch (index) { case 13 -> progress; case 14 -> input[0].getFluidAmount(); case 15 -> input[1].getFluidAmount();
            case 16 -> output.amount(); case 17 -> plan == null ? 250 : plan.recipe().ticks(); default -> 0; };
    }
    @Override public void telemetry(Map<String, Long> values) {
        values.put("progress_units", (long) progress); values.put("progress_target", (long) metric(17));
        values.put("input_1_mb", (long) metric(14)); values.put("input_2_mb", (long) metric(15)); values.put("output_mb", (long) metric(16));
    }
    @Override public CompoundTag save(HolderLookup.Provider lookup) {
        var tag = new CompoundTag(); tag.put("first", input[0].writeToNBT(lookup, new CompoundTag()));
        tag.put("second", input[1].writeToNBT(lookup, new CompoundTag())); tag.put("output", output.save(lookup));
        tag.putInt("progress", progress); tag.putString("recipe", recipeId); tag.putInt("first_index", lastTank); return tag;
    }
    @Override public void load(CompoundTag tag, HolderLookup.Provider lookup) {
        input[0].readFromNBT(lookup, tag.getCompound("first")); input[1].readFromNBT(lookup, tag.getCompound("second"));
        for (var tank : input) if (!tank.isEmpty() && !RefineryRecipes.validFluid(tank.getFluid().getFluid())) tank.setFluid(FluidStack.EMPTY);
        output.load(tag.getList("output", Tag.TAG_COMPOUND), lookup);
        progress = Mth.clamp(tag.getInt("progress"), 0, 499); recipeId = tag.getString("recipe"); lastTank = Mth.clamp(tag.getInt("first_index"), 0, 1);
        trackedInput = inventory.getStackInSlot(1).copyWithCount(1); plan = null;
    }
}
