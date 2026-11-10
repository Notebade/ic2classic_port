package ic2.core.machine;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.ItemStackHandler;

final class ReactorPlannerState {
    static final int EU_PER_STEP = 100;
    static final int HEAT_STEP = 500;
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
    private int maxHeat = ReactorRules.MAX_HEAT;
    private int output;

    ReactorPlannerState(Runnable changed) {
        this.changed = changed;
    }

    ItemStackHandler layout() {
        return layout;
    }

    boolean wantsStep() {
        return (step || running) && heat < maxHeat;
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
            case 2 -> step = true;
            case 3 -> running = !running && heat < maxHeat;
            case 4 -> reset();
            case 5 -> toggleMode();
            case 8 -> adjustStartingHeat(HEAT_STEP);
            case 9 -> adjustStartingHeat(-HEAT_STEP);
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
        running = step = false;
        changed.run();
    }

    void advance() {
        if (tanks != null) {
            tanks.fill(new FluidStack(Fluids.WATER, SteamReactorTanks.WATER_CAPACITY), FluidAction.EXECUTE);
        }
        var grid = new ReactorGrid(simulation, tanks);
        maxHeat = grid.hull(width).capacity();
        boolean damageTick = !steam || cycles % ReactorRules.CYCLE_TICKS == 0;
        var cycle = grid.run(width, heat, maxHeat, true, damageTick, cycles % 40 == 0, RandomSource.create(cycles));
        heat = cycle.heat();
        output = cycle.output();
        cycles++;
        step = false;
        if (tanks != null) {
            tanks.drain(SteamReactorTanks.STEAM_CAPACITY, FluidAction.EXECUTE);
        }
        if (heat >= maxHeat) {
            running = false;
        }
        changed.run();
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
            default -> 0;
        };
    }

    CompoundTag save(HolderLookup.Provider registries) {
        var tag = saveLayout(registries);
        tag.put("simulation", simulation.serializeNBT(registries));
        tag.putInt("cycles", cycles);
        tag.putInt("heat", heat);
        tag.putInt("output", output);
        return tag;
    }

    CompoundTag saveLayout(HolderLookup.Provider registries) {
        var tag = new CompoundTag();
        tag.put("layout", layout.serializeNBT(registries));
        tag.putInt("width", width);
        tag.putBoolean("steam", steam);
        tag.putInt("starting_heat", startingHeat);
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
        maxHeat = new ReactorGrid(simulation, tanks).hull(width).capacity();
    }
}
