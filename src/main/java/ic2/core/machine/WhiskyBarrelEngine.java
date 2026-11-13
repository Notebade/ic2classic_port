package ic2.core.machine;

import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Whisky-only barrel foundation; other drinks are deliberately left to the later brewing port. */
final class WhiskyBarrelEngine implements ChemicalEngine, IFluidHandler {
    private int water, grist, age, stage;
    private boolean sealed, opened;
    private final Runnable changed;
    private final ItemStackHandler inventory;
    WhiskyBarrelEngine(Runnable changed, ItemStackHandler inventory) { this.changed = changed; this.inventory = inventory; }
    @Override public Tick tick(ServerLevel level, BlockPos pos, int energy, int clock, ItemStackHandler ignored) {
        if (!sealed) {
            var stack = inventory.getStackInSlot(0);
            if (stack.is(ChemistryMaterials.GRIST.get()) && grist < 16) {
                int amount = Math.min(16 - grist, stack.getCount()); inventory.extractItem(0, amount, false); grist += amount;
            }
            ChemicalTransfer.container(inventory, 1, 4, this, false);
        }
        var next = WhiskyCycle.step(age, stage, sealed && !opened && water > 0 && grist > 0);
        age = next.age(); stage = next.stage();
        if (opened && water > 0 && inventory.getStackInSlot(2).is(ChemistryMaterials.MUG.get())) {
            var mug = ChemistryMaterials.whiskyMug(WhiskyCycle.grade(stage));
            if (ChemicalTransfer.fits(inventory, 3, mug)) {
                inventory.extractItem(2, 1, false); ChemicalTransfer.add(inventory, 3, mug);
                water = Math.max(0, water - 1000); // The original barrel also bottles a final partial mug.
                if (water == 0) reset();
            }
        }
        return new Tick(0, sealed && !opened);
    }
    private void reset() { water = 0; grist = 0; age = 0; stage = 0; sealed = false; opened = false; }
    @Override public boolean accepts(int slot, ItemStack stack) {
        return switch (slot) { case 0 -> stack.is(ChemistryMaterials.GRIST.get());
            case 1 -> FluidUtil.getFluidContained(stack).filter(fluid -> fluid.is(Fluids.WATER)).isPresent();
            case 2 -> stack.is(ChemistryMaterials.MUG.get()); default -> false; };
    }
    @Override public IItemHandler automation(Direction side) {
        return ChemicalTransfer.view(inventory, side == Direction.DOWN ? new int[]{3, 4} : new int[]{0, 1, 2, 3, 4}, slot -> slot >= 3);
    }
    @Override public boolean button(int button) {
        if (button == 9 && !sealed && water > 0 && grist > 0) { sealed = true; changed.run(); return true; }
        if (button == 10 && sealed && !opened) {
            water = WhiskyCycle.evaporated(water, stage, grist); opened = true;
            if (water == 0) reset(); changed.run(); return true;
        }
        return false;
    }
    @Override public IFluidHandler fluids() { return this; }
    @Override public int getTanks() { return 1; }
    @Override public FluidStack getFluidInTank(int tank) { return new FluidStack(Fluids.WATER, water); }
    @Override public int getTankCapacity(int tank) { return 32000; }
    @Override public boolean isFluidValid(int tank, FluidStack stack) { return !sealed && stack.is(Fluids.WATER); }
    @Override public int fill(FluidStack stack, FluidAction action) {
        if (!isFluidValid(0, stack)) return 0;
        int accepted = Math.min(stack.getAmount(), 32000 - water);
        if (action.execute() && accepted > 0) { water += accepted; changed.run(); }
        return accepted;
    }
    @Override public FluidStack drain(FluidStack stack, FluidAction action) { return stack.is(Fluids.WATER) ? drain(stack.getAmount(), action) : FluidStack.EMPTY; }
    @Override public FluidStack drain(int amount, FluidAction action) {
        if (sealed || amount <= 0) return FluidStack.EMPTY;
        int drained = Math.min(water, amount);
        if (action.execute() && drained > 0) { water -= drained; changed.run(); }
        return new FluidStack(Fluids.WATER, drained);
    }
    @Override public int metric(int index) {
        return switch (index) { case 13 -> age; case 14 -> water; case 15 -> grist; case 17 -> WhiskyCycle.STAGE_TICKS;
            case 19 -> stage; case 20 -> opened ? 2 : sealed ? 1 : 0; default -> 0; };
    }
    @Override public void telemetry(Map<String, Long> values) {
        values.put("age_ticks", (long) age); values.put("age_stage", (long) stage); values.put("whisky_grade", (long) WhiskyCycle.grade(stage));
        values.put("water_mb", (long) water); values.put("grist_units", (long) grist); values.put("opened", opened ? 1L : 0L);
    }
    @Override public CompoundTag save(HolderLookup.Provider lookup) {
        var tag = new CompoundTag(); tag.putInt("water", water); tag.putInt("grist", grist); tag.putInt("age", age); tag.putInt("stage", stage);
        tag.putBoolean("sealed", sealed); tag.putBoolean("opened", opened); return tag;
    }
    @Override public void load(CompoundTag tag, HolderLookup.Provider lookup) {
        water = Mth.clamp(tag.getInt("water"), 0, 32000); grist = Mth.clamp(tag.getInt("grist"), 0, 16);
        age = Mth.clamp(tag.getInt("age"), 0, WhiskyCycle.STAGE_TICKS - 1); stage = Mth.clamp(tag.getInt("stage"), 0, 50);
        sealed = tag.getBoolean("sealed"); opened = sealed && tag.getBoolean("opened");
    }
}
