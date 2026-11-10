package ic2.core.machine;

import java.util.Arrays;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.ItemStackHandler;

final class ReactorPlannerState {
    static final int EU_PER_BATCH = 100;
    private static final int MAX_CYCLES_PER_TICK = 100;
    static final int HEAT_STEP = 500;
    private static final int[] CYCLE_LIMITS = {0, 20, 100, 500, 1_000, 5_000, 10_000, 20_000, 100_000};
    private final Runnable changed;
    private final ItemStackHandler layout = new ItemStackHandler(ReactorRules.SLOT_COUNT);
    private ItemStackHandler simulation = new ItemStackHandler(ReactorRules.SLOT_COUNT);
    private SteamReactorTanks tanks;
    private int width = ReactorRules.BASE_COLUMNS;
    private boolean steam;
    private boolean running;
    private boolean step;
    private int cycles;
    private int heat;
    private int startingHeat;
    private int cycleLimitIndex;
    private int cyclesPerTick = 1;
    private int maxHeat = ReactorRules.MAX_HEAT;
    private int output;
    private long totalProduction;
    private long sampledHeat;
    private int heatSamples;
    private int peakHeat = -1;

    ReactorPlannerState(Runnable changed) {
        this.changed = changed;
    }

    ItemStackHandler layout() {
        return layout;
    }

    boolean wantsStep() {
        return (step || running) && canAdvance();
    }

    private boolean canAdvance() {
        return heat < maxHeat && cycles < (cycleLimit() == 0 ? Integer.MAX_VALUE : cycleLimit());
    }

    private int cycleLimit() {
        return CYCLE_LIMITS[cycleLimitIndex];
    }

    boolean running() {
        return running;
    }

    void setComponent(int slot, ItemStack stack) {
        if (slot % ReactorRules.COLUMNS < width && (stack.isEmpty() || ReactorGrid.accepts(stack, steam))) {
            layout.setStackInSlot(slot, stack.copy());
            reset();
        }
    }

    boolean command(int command) {
        switch (command) {
            case 0 -> resize(-1);
            case 1 -> resize(1);
            case 2 -> {
                if (!canAdvance()) {
                    return false;
                }
                step = true;
            }
            case 3 -> running = !running && canAdvance();
            case 4 -> reset();
            case 5 -> toggleMode();
            case 8 -> adjustStartingHeat(HEAT_STEP);
            case 9 -> adjustStartingHeat(-HEAT_STEP);
            case 10 -> adjustCycleLimit(1);
            case 11 -> adjustCycleLimit(-1);
            case 12 -> cyclesPerTick = Math.min(MAX_CYCLES_PER_TICK, cyclesPerTick + 1);
            case 13 -> cyclesPerTick = Math.max(1, cyclesPerTick - 1);
            default -> { return false; }
        }
        changed.run();
        return true;
    }

    private void resize(int amount) {
        int resized = Math.clamp(width + amount, ReactorRules.BASE_COLUMNS, ReactorRules.COLUMNS);
        if (resized != width) {
            width = resized;
            clearInactive();
            reset();
        }
    }

    private void toggleMode() {
        steam = !steam;
        clearInactive();
        reset();
    }

    private void adjustStartingHeat(int amount) {
        int adjusted = Math.clamp(startingHeat + amount, 0, maxHeat - 1);
        if (adjusted != startingHeat) {
            startingHeat = adjusted;
            reset();
        }
    }

    private void adjustCycleLimit(int amount) {
        cycleLimitIndex = Math.clamp(cycleLimitIndex + amount, 0, CYCLE_LIMITS.length - 1);
        if (!canAdvance()) {
            running = step = false;
        }
    }

    private void clearInactive() {
        for (int slot = 0; slot < layout.getSlots(); slot++) {
            if (slot % ReactorRules.COLUMNS >= width || !ReactorGrid.accepts(layout.getStackInSlot(slot), steam)) {
                layout.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    private void reset() {
        simulation = new ItemStackHandler(ReactorRules.SLOT_COUNT);
        for (int slot = 0; slot < layout.getSlots(); slot++) {
            simulation.setStackInSlot(slot, layout.getStackInSlot(slot).copy());
        }
        tanks = steam ? new SteamReactorTanks(() -> {}) : null;
        maxHeat = new ReactorGrid(simulation, tanks).hull(width).capacity();
        startingHeat = Math.clamp(startingHeat, 0, maxHeat - 1);
        heat = startingHeat;
        output = cycles = 0;
        totalProduction = 0;
        sampledHeat = 0;
        heatSamples = 0;
        peakHeat = -1;
        running = step = false;
        changed.run();
    }

    void advanceBatch() {
        int batchSize = running ? cyclesPerTick : 1;
        for (int index = 0; index < batchSize && canAdvance(); index++) {
            advanceCycle();
        }
        step = false;
        if (!canAdvance()) {
            running = false;
        }
        changed.run();
    }

    private void advanceCycle() {
        if (tanks != null) {
            tanks.fill(new FluidStack(Fluids.WATER, SteamReactorTanks.WATER_CAPACITY), FluidAction.EXECUTE);
        }
        var grid = new ReactorGrid(simulation, tanks);
        maxHeat = grid.hull(width).capacity();
        boolean damageTick = !steam || cycles % ReactorRules.CYCLE_TICKS == 0;
        var cycle = grid.run(width, heat, maxHeat, true, damageTick, cycles % 40 == 0, RandomSource.create(cycles));
        heat = cycle.heat();
        sampledHeat += heat;
        heatSamples++;
        peakHeat = Math.max(peakHeat, heat);
        output = cycle.output();
        totalProduction += (long) output * (steam ? 1 : ReactorRules.CYCLE_TICKS);
        cycles++;
        if (tanks != null) {
            tanks.drain(SteamReactorTanks.STEAM_CAPACITY, FluidAction.EXECUTE);
        }
    }

    int data(int index) {
        return switch (index) {
            case 0 -> width;
            case 1 -> steam ? 1 : 0;
            case 2 -> running ? 1 : 0;
            case 3 -> heat & 0xffff;
            case 4 -> heat >>> 16;
            case 5 -> maxHeat & 0xffff;
            case 6 -> maxHeat >>> 16;
            case 7 -> output & 0xffff;
            case 8 -> output >>> 16;
            case 9 -> cycles & 0xffff;
            case 10 -> cycles >>> 16;
            case 11 -> startingHeat & 0xffff;
            case 12 -> startingHeat >>> 16;
            case 13 -> cycleLimit() & 0xffff;
            case 14 -> cycleLimit() >>> 16;
            case 15 -> cyclesPerTick;
            case 16, 17, 18, 19 -> (int) (totalProduction >>> ((index - 16) * 16)) & 0xffff;
            case 20, 21 -> (averageHeat() >>> ((index - 20) * 16)) & 0xffff;
            case 22, 23 -> (peakHeat >>> ((index - 22) * 16)) & 0xffff;
            default -> 0;
        };
    }

    private int averageHeat() {
        return heatSamples == 0 ? -1 : (int) (sampledHeat / heatSamples);
    }

    CompoundTag save(HolderLookup.Provider registries) {
        var tag = saveLayout(registries);
        tag.put("simulation", simulation.serializeNBT(registries));
        tag.putInt("cycles", cycles);
        tag.putInt("heat", heat);
        tag.putInt("output", output);
        tag.putLong("total_production", totalProduction);
        tag.putLong("sampled_heat", sampledHeat);
        tag.putInt("heat_samples", heatSamples);
        tag.putInt("peak_heat", peakHeat);
        return tag;
    }

    CompoundTag saveLayout(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        tag.put("layout", layout.serializeNBT(registries));
        tag.putInt("width", width);
        tag.putBoolean("steam", steam);
        tag.putInt("starting_heat", startingHeat);
        tag.putInt("cycle_limit", cycleLimit());
        tag.putInt("cycles_per_tick", cyclesPerTick);
        return tag;
    }

    void loadLayout(HolderLookup.Provider registries, CompoundTag tag) {
        if (tag.contains("layout")) {
            var saved = tag.getCompound("layout").copy();
            saved.putInt("Size", ReactorRules.SLOT_COUNT);
            layout.deserializeNBT(registries, saved);
        }
        width = Math.clamp(tag.getInt("width"), ReactorRules.BASE_COLUMNS, ReactorRules.COLUMNS);
        steam = tag.getBoolean("steam");
        startingHeat = tag.getInt("starting_heat");
        cycleLimitIndex = Math.max(0, Arrays.binarySearch(CYCLE_LIMITS, tag.getInt("cycle_limit")));
        cyclesPerTick = Math.clamp(tag.getInt("cycles_per_tick"), 1, MAX_CYCLES_PER_TICK);
        clearInactive();
        reset();
    }

    void load(HolderLookup.Provider registries, CompoundTag tag) {
        loadLayout(registries, tag);
        if (tag.contains("simulation")) {
            simulation.deserializeNBT(registries, tag.getCompound("simulation"));
        }
        cycles = Math.max(0, tag.getInt("cycles"));
        heat = Math.max(0, tag.getInt("heat"));
        output = Math.max(0, tag.getInt("output"));
        totalProduction = Math.max(0, tag.getLong("total_production"));
        heatSamples = Math.clamp(tag.getInt("heat_samples"), 0, cycles);
        sampledHeat = Math.clamp(tag.getLong("sampled_heat"), 0, (long) heatSamples * Integer.MAX_VALUE);
        peakHeat = tag.contains("peak_heat") ? Math.max(-1, tag.getInt("peak_heat")) : -1;
        maxHeat = new ReactorGrid(simulation, tanks).hull(width).capacity();
    }
}
