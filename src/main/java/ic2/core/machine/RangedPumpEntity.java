package ic2.core.machine;

import ic2.api.item.WorldScanner;
import ic2.api.machine.MachineSnapshot;
import ic2.core.item.RechargeableItem;
import java.util.ArrayDeque;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

final class RangedPumpEntity extends BlockEntity implements EnergySink, MachineSnapshot.Provider, ExtractionDevice {
    private int energy, progress, delay;
    private Target target;
    private final ArrayDeque<Target> targets = new ArrayDeque<>();
    private final ArrayDeque<BlockPos> search = new ArrayDeque<>();
    private final HashSet<BlockPos> visited = new HashSet<>();
    private BlockPos searchStart;
    private int searchRadius;
    private boolean searching, searched;
    private final PumpTank tank = new PumpTank(this::setChanged);
    final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 ? RechargeableItem.supports(stack, 128, false) : slot == 1 ? stack.getItem() instanceof BlockItem
                : slot == 2 && stack.getItem() instanceof WorldScanner scanner && scanner.scansFluids();
        }
        @Override public int getSlotLimit(int slot) { return slot == 1 ? 64 : 1; }
        @Override protected void onContentsChanged(int slot) {
            if (slot == 2) { progress = 0; target = null; targets.clear(); resetSearch(); }
            setChanged();
        }
    };
    private final ContainerData data = new ContainerData() {
        @Override public int getCount() { return 4; }
        @Override public int get(int index) { return switch (index) { case 0 -> energy; case 1 -> progress; case 2 -> tank.getFluidAmount(); case 3 -> 5000; default -> 0; }; }
        @Override public void set(int index, int value) {}
    };
    RangedPumpEntity(BlockPos pos, BlockState state) { super(ExtractionMachines.RANGED_ENTITY.get(), pos, state); }
    private boolean live() { return level != null && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this; }
    private boolean hasPipe() { return inventory.getStackInSlot(1).is(MiningWorld.pipe().getItem()); }
    private BlockPos tip() { return MiningWorld.tip(level, worldPosition); }
    private void resetSearch() { search.clear(); visited.clear(); searchStart = null; searching = false; searched = false; }
    @Override public void tick() {
        if (!live()) return;
        chargeItems(); exportFluids();
        boolean active = false;
        if (delay > 0) { delay--; setChanged(); }
        else if (energy >= 100 && inventory.getStackInSlot(2).getItem() instanceof WorldScanner scanner && scanner.scansFluids()
            && (hasPipe() || target != null || !targets.isEmpty())) { work(scanner); active = true; }
        else if (inventory.getStackInSlot(2).isEmpty() && energy >= 2 && tip().getY() < worldPosition.getY()) {
            energy -= 2; progress++; targets.clear(); target = null; resetSearch();
            if (progress >= 5) { progress = 0; MiningWorld.retract(level, worldPosition, inventory, 1); }
            active = true; setChanged();
        }
        if (getBlockState().getValue(BlockStateProperties.LIT) != active)
            level.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.LIT, active), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
    private void chargeItems() {
        var stack = inventory.getStackInSlot(0);
        if (RechargeableItem.supports(stack, 128, false)) {
            var battery = (ic2.api.item.ElectricItem) stack.getItem(); int amount = Math.min(5000 - energy, Math.min(battery.charge(stack), battery.transferRate()));
            if (amount > 0) { battery.setCharge(stack, battery.charge(stack) - amount); energy += amount; setChanged(); }
        }
        stack = inventory.getStackInSlot(2);
        if (stack.getItem() instanceof ic2.api.item.ElectricItem scanner && RechargeableItem.supports(stack, 128)) {
            int amount = Math.min(energy, scanner.capacity() - scanner.charge(stack));
            if (amount > 0) { scanner.setCharge(stack, scanner.charge(stack) + amount); energy -= amount; setChanged(); }
        }
    }
    private void exportFluids() {
        if (tank.isEmpty()) return;
        for (var direction : Direction.values()) {
            if (direction == Direction.DOWN || !level.hasChunkAt(worldPosition.relative(direction))) continue;
            var other = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(direction), direction.getOpposite());
            if (other != null) FluidUtil.tryFluidTransfer(other, tank, 1000, true);
            if (tank.isEmpty()) return;
        }
    }
    private void work(WorldScanner scanner) {
        if (target == null) { chooseTarget(scanner); return; }
        if (!valid(target)) { target = null; progress = 0; setChanged(); return; }
        if (progress < 5) { progress++; energy -= 10; setChanged(); return; }
        boolean vertical = target.position().getX() == worldPosition.getX() && target.position().getZ() == worldPosition.getZ();
        if (vertical && !hasPipe()) return;
        var fluid = target.fluid();
        if (!fluid.isEmpty() && !tank.acceptCollected(fluid, true)) { delay = 5; return; }
        if (!target.dig()) {
            var state = level.getBlockState(target.position());
            if (!(state.getBlock() instanceof BucketPickup pickup) || pickup.pickupBlock(null, level, target.position(), state).isEmpty()) { target = null; progress = 0; return; }
            tank.acceptCollected(fluid, false);
        } else level.setBlock(target.position(), Blocks.AIR.defaultBlockState(), 2 | 16);
        energy -= 10; progress = 0;
        if (vertical) {
            MiningWorld.install(level, target.position()); inventory.extractItem(1, 1, false);
            targets.clear(); resetSearch();
        }
        target = null; setChanged();
    }
    private boolean valid(Target value) {
        var pos = value.position();
        if (pos.getY() < level.getMinBuildHeight() || pos.getY() >= level.getMaxBuildHeight() || !level.hasChunkAt(pos)) return false;
        var state = level.getBlockState(pos);
        return value.dig() ? state.isAir() || !state.getFluidState().isEmpty() && !state.getFluidState().isSource() : state == value.state();
    }
    private void chooseTarget(WorldScanner scanner) {
        var tip = tip();
        if (tip.getY() < worldPosition.getY() && targets.isEmpty() && !searched && !searching) beginSearch(scanner, tip);
        if (searching) { advanceSearch(scanner); if (searching) return; }
        while (!targets.isEmpty()) {
            var next = targets.removeFirst();
            if (valid(next)) { target = next; searched = false; setChanged(); return; }
        }
        searched = false;
        var below = tip.below();
        if (!hasPipe() || below.getY() < level.getMinBuildHeight() || !level.hasChunkAt(below)) { delay = 50; return; }
        var state = level.getBlockState(below);
        var next = new Target(below, state, state.getFluidState().isEmpty() || !state.getFluidState().isSource());
        if (valid(next) && (next.dig() || state.getBlock() instanceof BucketPickup)) target = next;
        else delay = 50;
        setChanged();
    }
    private void beginSearch(WorldScanner scanner, BlockPos tip) {
        searchRadius = Math.clamp(scanner.scanRadius(inventory.getStackInSlot(2), true), 0, 32);
        searched = true;
        if (searchRadius == 0) return;
        searchStart = tip; visited.add(tip); search.add(tip); searching = true; setChanged();
    }
    private void advanceSearch(WorldScanner scanner) {
        for (int budget = 0; budget < 32 && !search.isEmpty(); budget++) {
            var pos = search.removeFirst();
            if (!level.hasChunkAt(pos)) continue;
            var state = level.getBlockState(pos);
            if (!pos.equals(searchStart) && !state.isAir() && !(state.getBlock() instanceof BucketPickup)) continue;
            var fluid = state.getFluidState();
            if (state.getBlock() instanceof BucketPickup && !fluid.isEmpty()
                && (scanner.isValuable(inventory.getStackInSlot(2), state, level, pos) || !fluid.isSource())) targets.add(new Target(pos, state, !fluid.isSource()));
            for (var direction : Direction.values()) {
                if (direction == Direction.DOWN) continue;
                var next = pos.relative(direction);
                if (next.getY() < searchStart.getY() || next.getY() > worldPosition.getY()
                    || Math.abs(next.getX() - worldPosition.getX()) > searchRadius || Math.abs(next.getZ() - worldPosition.getZ()) > searchRadius) continue;
                if (visited.add(next)) search.add(next);
            }
        }
        if (search.isEmpty()) { searching = false; visited.clear(); searchStart = null; }
    }
    IItemHandler automation(Direction side) {
        return !live() || side == Direction.DOWN || side == Direction.UP ? null : GuardedCapabilities.items(() -> live()
            ? new CombinedInvWrapper(new BatterySlot(inventory, 0, false), new RangedWrapper(inventory, 1, 3)) : null);
    }
    IFluidHandler fluidOutput(Direction side) { return !live() || side == Direction.DOWN ? null : GuardedCapabilities.fluids(() -> live() ? tank : null); }
    @Override public boolean canReceiveEnergy(Direction side, int output, int packet) { return live() && packet > 0 && packet <= 128 && energy < 5000; }
    @Override public int receiveEnergy(int amount) { if (!live()) return 0; int received = Math.clamp(amount, 0, 5000 - energy); energy += received; if (received > 0) setChanged(); return received; }
    @Override public MachineSnapshot machineInfo() { return MachineTelemetry.snapshot(this, energy, 5000, MachineSnapshot.EnergyUnit.EU,
        java.util.Map.of("progress_units", (long) progress, "progress_target", 5L, "input_packet_eu", 128L, "fluid_mb", (long) tank.getFluidAmount(), "queued_targets", (long) targets.size(), "searching", searching ? 1L : 0L, "pipe_tip_y", (long) tip().getY())); }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory player, Player owner) { return new ExtractionMenu(ExtractionMachines.RANGED_MENU.get(), id, player, this, inventory, data, -1); }
    @Override public void dropContents() { for (int slot = 0; slot < inventory.getSlots(); slot++) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), inventory.getStackInSlot(slot)); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup); tag.putInt("energy", energy); tag.putInt("progress", progress); tag.putInt("delay", delay);
        tag.put("inventory", inventory.serializeNBT(lookup)); tag.put("tank", tank.writeToNBT(lookup, new CompoundTag()));
        if (target != null) tag.put("target", target.save());
        var queue = new ListTag(); for (var value : targets) queue.add(value.save()); tag.put("targets", queue);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup); inventory.deserializeNBT(lookup, tag.getCompound("inventory"));
        energy = Math.clamp(tag.getInt("energy"), 0, 5000); progress = Math.clamp(tag.getInt("progress"), 0, 5); delay = Math.clamp(tag.getInt("delay"), 0, 50);
        tank.readFromNBT(lookup, tag.getCompound("tank")); target = tag.contains("target") ? Target.load(tag.getCompound("target"), lookup) : null;
        targets.clear(); for (var value : tag.getList("targets", 10)) { if (targets.size() >= 65536) break; targets.add(Target.load((CompoundTag) value, lookup)); }
        resetSearch();
    }
    private record Target(BlockPos position, BlockState state, boolean dig) {
        FluidStack fluid() { return dig ? FluidStack.EMPTY : new FluidStack(state.getFluidState().getType(), 1000); }
        CompoundTag save() { var tag = new CompoundTag(); tag.putLong("position", position.asLong()); tag.put("state", NbtUtils.writeBlockState(state)); tag.putBoolean("dig", dig); return tag; }
        static Target load(CompoundTag tag, HolderLookup.Provider lookup) { return new Target(BlockPos.of(tag.getLong("position")), NbtUtils.readBlockState(lookup.lookupOrThrow(Registries.BLOCK), tag.getCompound("state")), tag.getBoolean("dig")); }
    }
}
