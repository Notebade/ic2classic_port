package ic2.core.machine;

import ic2.api.machine.MachineSnapshot;
import ic2.api.machine.MachineUpgrade;
import ic2.core.item.RechargeableItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

final class ColossalEntity extends BlockEntity implements MenuProvider, EnergySink, MachineSnapshot.Provider, ic2.api.machine.UpgradeContext.ConnectionProvider {
    final ColossalMachines.Kind kind = ((ColossalBlock) getBlockState().getBlock()).kind;
    private int energy, edge;
    private float experience;
    private final int[] progress = new int[8];
    private final net.minecraft.core.NonNullList<ItemStack> trackedInputs = net.minecraft.core.NonNullList.withSize(8, ItemStack.EMPTY);
    private List<BlockPos> parts = List.of();
    private boolean processing;
    final ItemStackHandler inventory = new ItemStackHandler(19) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            if (slot < 8) return slot < lanes() && ColossalRecipes.find(kind, stack, level) != null;
            if (slot == 16) return RechargeableItem.supports(stack.copyWithCount(1), modifiers().packet(), false);
            return slot >= 17 && stack.getItem() instanceof MachineUpgrade upgrade && upgrade.category(stack) != MachineUpgrade.Category.RECIPE;
        }
        @Override protected void onContentsChanged(int slot) {
            if (slot < 8) {
                var current = getStackInSlot(slot);
                if (!ItemStack.isSameItemSameComponents(current, trackedInputs.get(slot))) progress[slot] = 0;
                trackedInputs.set(slot, current.copy());
            }
            if (slot >= 17) energy = Math.min(energy, modifiers().capacity());
            setChanged();
        }
    };
    final FluidTank water = new FluidTank(16000, stack -> stack.getFluid() == Fluids.WATER) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    final FluidTank steam = new FluidTank(16000, SteamFluids::isSteam) {
        @Override protected void onContentsChanged() { setChanged(); }
    };
    final IFluidHandler tanks = new IFluidHandler() {
        @Override public int getTanks() { return 2; }
        @Override public FluidStack getFluidInTank(int tank) { return (tank == 0 ? water : steam).getFluid(); }
        @Override public int getTankCapacity(int tank) { return 16000; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return tank == 0 && water.isFluidValid(stack); }
        @Override public int fill(FluidStack stack, FluidAction action) { return water.fill(stack, action); }
        @Override public FluidStack drain(FluidStack stack, FluidAction action) { return steam.drain(stack, action); }
        @Override public FluidStack drain(int amount, FluidAction action) { return steam.drain(amount, action); }
    };
    final ContainerData data = new ContainerData() {
        @Override public int getCount() { return 28; }
        @Override public int get(int index) {
            int value = switch (index / 2) { case 0 -> energy; case 1 -> modifiers().capacity(); case 2 -> edge;
                case 3 -> water.getFluidAmount(); case 4 -> steam.getFluidAmount(); case 5 -> processing ? 1 : 0; default -> progress[index / 2 - 6]; };
            return index % 2 == 0 ? value & 65535 : value >>> 16;
        }
        @Override public void set(int index, int value) {}
    };

    ColossalEntity(BlockPos pos, BlockState state) { super(ColossalMachines.ENTITY.get(), pos, state); }
    private MachineModifiers modifiers() { return MachineModifiers.read(inventory, 17, 100000, 4); }
    private ic2.api.machine.UpgradeContext upgradeContext() {
        return new ic2.api.machine.UpgradeContext((ServerLevel) level, worldPosition,
            new RangedWrapper(inventory, 0, lanes()), new RangedWrapper(inventory, 8, 8 + lanes()), java.util.Optional.of(tanks));
    }
    private void tickUpgrades() {
        for (int slot = 17; slot < 19; slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof MachineUpgrade upgrade && upgrade.tickUpgrade(stack, upgradeContext())) setChanged();
        }
    }
    int lanes() { return ColossalCycle.lanes(edge); }
    boolean contains(BlockPos pos) { return parts.contains(pos); }
    @Override public List<BlockPos> upgradeConnectionPositions() {
        var positions = new ArrayList<BlockPos>(); positions.add(worldPosition); positions.addAll(parts); return List.copyOf(positions);
    }
    boolean geometryValid() {
        if (edge == 0 || parts.size() != edge * edge * edge - 1) return false;
        for (var pos : parts) if (!level.hasChunkAt(pos) || !level.getBlockState(pos).is(ColossalMachines.BASE.get())) return false;
        return true;
    }
    boolean live() { return level != null && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this && geometryValid(); }

    private void validateStructure() {
        int previousEdge = edge;
        var previousParts = parts;
        edge = 0; parts = List.of();
        var forward = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING).getOpposite();
        var right = forward.getClockWise();
        for (int candidate = 4; candidate >= 2; candidate--) {
            var found = new ArrayList<BlockPos>();
            boolean valid = true;
            for (int y = 0; y < candidate && valid; y++) for (int z = 0; z < candidate && valid; z++) for (int x = 0; x < candidate; x++) {
                if (x == 0 && y == 0 && z == 0) continue;
                var pos = worldPosition.relative(right, x).relative(forward, z).below(y);
                if (!level.hasChunkAt(pos) || !(level.getBlockEntity(pos) instanceof ColossalPartEntity part) || !part.canClaim(worldPosition)) { valid = false; break; }
                found.add(pos);
            }
            if (!valid) continue;
            edge = candidate; parts = List.copyOf(found);
            if (edge == previousEdge && parts.equals(previousParts)) return;
            for (var pos : parts) ((ColossalPartEntity) level.getBlockEntity(pos)).claim(worldPosition);
            setChanged(); level.invalidateCapabilities(worldPosition); return;
        }
    }

    void tick() {
        validateStructure();
        if (!live()) { setProcessing(false); return; }
        tickUpgrades();
        if (!live()) { setProcessing(false); return; }
        discharge();
        var recipes = new ColossalRecipes.Batch[lanes()];
        int active = 0;
        for (int lane = 0; lane < lanes(); lane++) { recipes[lane] = available(lane); if (recipes[lane] != null) active++; }
        int cost = MachineUpgradeRules.scale(30, modifiers().demand());
        boolean ready = active > 0 && ColossalCycle.fluidsReady(water.getFluidAmount(), steam.getFluidAmount(), active)
            && modifiers().redstoneAllowed(false, level.hasNeighborSignal(worldPosition))
            && energy >= (processing ? (long) active * cost : modifiers().capacity() / 10);
        boolean worked = false;
        for (int lane = 0; lane < lanes(); lane++) {
            var recipe = recipes[lane];
            boolean run = ready && recipe != null && energy >= cost;
            int step = kind == ColossalMachines.Kind.RECYCLER ? 6000 : 1000;
            int previous = progress[lane];
            progress[lane] = recipe == null ? 0 : ColossalCycle.progress(progress[lane], run, step);
            if (run) {
                worked = true; energy -= cost;
                if (progress[lane] == 6000) finish(lane, recipe);
            }
            if (run || previous != progress[lane]) setChanged();
        }
        setProcessing(worked);
    }

    private ColossalRecipes.Batch available(int lane) {
        var input = inventory.getStackInSlot(lane);
        var recipe = ColossalRecipes.find(kind, input, level);
        if (recipe == null || input.getCount() < recipe.inputCount()) return null;
        var output = inventory.getStackInSlot(8 + lane);
        return output.isEmpty() || ItemStack.isSameItemSameComponents(output, recipe.output()) && output.getCount() + recipe.output().getCount() <= output.getMaxStackSize() ? recipe : null;
    }

    private void finish(int lane, ColossalRecipes.Batch recipe) {
        boolean success = level.random.nextInt(recipe.chanceDenominator()) < (kind == ColossalMachines.Kind.RECYCLER ? 5 : 1);
        if (success) {
            var output = inventory.getStackInSlot(8 + lane);
            inventory.setStackInSlot(8 + lane, recipe.output().copyWithCount(output.getCount() + recipe.output().getCount()));
        }
        inventory.extractItem(lane, recipe.inputCount(), false);
        progress[lane] = 0;
        water.drain(50, IFluidHandler.FluidAction.EXECUTE);
        steam.fill(new FluidStack(SteamFluids.steam(), 25), IFluidHandler.FluidAction.EXECUTE);
        experience += recipe.experience();
        for (int slot = 17; slot < 19; slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof MachineUpgrade upgrade) upgrade.recipeFinished(stack, upgradeContext(), success ? recipe.output().copy() : ItemStack.EMPTY, recipe.experience());
        }
    }

    private void discharge() {
        var stack = inventory.getStackInSlot(16);
        if (!RechargeableItem.supports(stack, modifiers().packet(), false)) return;
        var cell = (RechargeableItem) stack.getItem();
        int room = (int) ((long) modifiers().capacity() * 9 / 10 - energy);
        int amount = Math.max(0, Math.min(room, Math.min(cell.transferRate(), cell.charge(stack))));
        if (amount > 0) { cell.setCharge(stack, cell.charge(stack) - amount); energy += amount; setChanged(); }
    }
    private void setProcessing(boolean active) {
        processing = active;
        if (getBlockState().getValue(BlockStateProperties.LIT) != active)
            level.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.LIT, active), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
    IItemHandler automation(Direction side) {
        return side == Direction.DOWN ? new RangedWrapper(inventory, 8, 8 + lanes()) : new RangedWrapper(inventory, 0, lanes()) {
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        };
    }
    IItemHandler items(Direction side) { return GuardedCapabilities.items(() -> live() ? automation(side) : null); }
    IFluidHandler fluids() { return GuardedCapabilities.fluids(() -> live() ? tanks : null); }
    @Override public boolean canReceiveEnergy(Direction side, int sourceStorageOutput, int packet) { return live() && packet > 0 && packet <= modifiers().packet() && energy < modifiers().capacity(); }
    @Override public int receiveEnergy(int offered) {
        if (!live()) return 0;
        int accepted = Math.clamp(offered, 0, Math.min(modifiers().packet(), modifiers().capacity() - energy));
        energy += accepted; if (accepted > 0) setChanged(); return accepted;
    }
    void open(Player player) {
        validateStructure();
        if (live()) player.openMenu(this, buffer -> { buffer.writeVarInt(lanes()); buffer.writeEnum(kind); });
    }
    @Override public Component getDisplayName() { return Component.translatable("block.ic2." + kind.id()); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) { return new ColossalMenu(id, playerInventory, lanes(), kind, this, inventory, data); }
    void releaseExperience() {
        int whole = (int) experience;
        if (whole > 0) { net.minecraft.world.entity.ExperienceOrb.award((ServerLevel) level, worldPosition.getCenter(), whole); experience -= whole; setChanged(); }
    }
    void dropContents() {
        if (level.isClientSide) return;
        for (int slot = 0; slot < inventory.getSlots(); slot++) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), inventory.getStackInSlot(slot));
        releaseExperience();
    }
    @Override public MachineSnapshot machineInfo() {
        var values = new java.util.HashMap<String, Long>();
        values.put("formed", live() ? 1L : 0L); values.put("structure_edge", (long) edge); values.put("recipe_lanes", (long) lanes());
        values.put("water_mb", (long) water.getFluidAmount()); values.put("steam_mb", (long) steam.getFluidAmount());
        values.put("progress_target", 6000L); values.put("processing", processing ? 1L : 0L);
        values.put("input_packet_eu", (long) modifiers().packet()); values.put("cost_eu_per_lane", 30L);
        for (int lane = 0; lane < 8; lane++) values.put("lane_" + lane + "_progress", (long) progress[lane]);
        return MachineTelemetry.snapshot(this, energy, modifiers().capacity(), MachineSnapshot.EnergyUnit.EU, values);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup); tag.put("inventory", inventory.serializeNBT(lookup)); tag.putInt("energy", energy);
        tag.putIntArray("progress", progress); tag.putFloat("experience", experience);
        tag.put("water", water.writeToNBT(lookup, new CompoundTag())); tag.put("steam", steam.writeToNBT(lookup, new CompoundTag()));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup);
        var stored = tag.getCompound("inventory").copy(); stored.putInt("Size", 19); inventory.deserializeNBT(lookup, stored);
        energy = Math.clamp(tag.getInt("energy"), 0, modifiers().capacity()); experience = Math.max(0, tag.getFloat("experience"));
        var saved = tag.getIntArray("progress"); for (int lane = 0; lane < 8; lane++) progress[lane] = lane < saved.length ? Math.clamp(saved[lane], 0, 5999) : 0;
        water.readFromNBT(lookup, tag.getCompound("water")); steam.readFromNBT(lookup, tag.getCompound("steam"));
        edge = 0; parts = List.of(); processing = false;
    }
}
