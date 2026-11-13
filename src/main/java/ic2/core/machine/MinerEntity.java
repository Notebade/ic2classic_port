package ic2.core.machine;

import ic2.api.item.MiningDrill;
import ic2.api.item.WorldScanner;
import ic2.api.machine.MachineSnapshot;
import ic2.core.item.RechargeableItem;
import java.util.ArrayDeque;
import java.util.HashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.ItemStackHandler;

final class MinerEntity extends BlockEntity implements EnergySink, MachineSnapshot.Provider, ExtractionDevice {
    private int energy, progress;
    private BlockPos desired, working;
    private boolean blocked;
    private final ArrayDeque<BlockPos> targets = new ArrayDeque<>();
    final ItemStackHandler inventory = new ItemStackHandler(4) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case 0 -> RechargeableItem.supports(stack, 128, false);
                case 1 -> stack.getItem() instanceof WorldScanner scanner && !scanner.scansFluids();
                case 2 -> stack.getItem() instanceof BlockItem;
                case 3 -> stack.getItem() instanceof MiningDrill;
                default -> false;
            };
        }
        @Override public int getSlotLimit(int slot) { return slot == 2 ? 64 : 1; }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final ContainerData data = new ContainerData() {
        @Override public int getCount() { return 6; }
        @Override public int get(int index) { return switch (index) { case 0 -> energy; case 1 -> progress; case 2 -> blocked ? 1 : 0; case 3 -> 10000; case 4 -> targets.size(); case 5 -> tip().getY(); default -> 0; }; }
        @Override public void set(int index, int value) {}
    };
    MinerEntity(BlockPos pos, BlockState state) { super(ExtractionMachines.MINER_ENTITY.get(), pos, state); }
    private boolean live() { return level != null && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this; }
    private ItemStack pipeStack() { return MiningWorld.pipe(); }
    private boolean hasPipe() { return inventory.getStackInSlot(2).is(pipeStack().getItem()); }
    private BlockPos tip() { return MiningWorld.tip(level, worldPosition); }
    @Override public void tick() {
        if (!live()) return;
        chargeTools();
        boolean active = false;
        var tool = inventory.getStackInSlot(3);
        if (tool.getItem() instanceof MiningDrill drill && drill.canUse(tool) && energy >= 100
            && (hasPipe() || desired != null || !targets.isEmpty()) && !level.hasNeighborSignal(worldPosition)) active = work(tool, drill);
        else if (tool.isEmpty() && energy > 0 && tip().getY() < worldPosition.getY()) {
            active = true; energy--; progress++; desired = null; working = null; targets.clear();
            if (progress >= 5) { progress = 0; retract(); }
            setChanged();
        }
        if (getBlockState().getValue(BlockStateProperties.LIT) != active)
            level.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.LIT, active), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
    private void chargeTools() {
        var stack = inventory.getStackInSlot(0);
        if (RechargeableItem.supports(stack, 128, false)) {
            var battery = (ic2.api.item.ElectricItem) stack.getItem(); int amount = Math.min(9000 - energy, Math.min(battery.charge(stack), battery.transferRate()));
            if (amount > 0) { battery.setCharge(stack, battery.charge(stack) - amount); energy += amount; setChanged(); }
        }
        if (energy < 100) return;
        for (int slot : new int[]{1, 3}) {
            stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof ic2.api.item.ElectricItem item) {
                int amount = Math.min(energy, item.capacity() - item.charge(stack));
                if (amount > 0) { item.setCharge(stack, item.charge(stack) + amount); energy -= amount; setChanged(); }
            }
        }
    }
    private boolean work(ItemStack tool, MiningDrill drill) {
        var tip = tip();
        if (desired == null) {
            if (targets.isEmpty() && tip.getY() < worldPosition.getY()) scan(tip, tool, drill);
            desired = targets.pollFirst();
            if (desired == null) desired = tip.below();
        }
        var next = obstruction(desired, tip);
        if (next.getY() < level.getMinBuildHeight() || !level.hasChunkAt(next)) return false;
        if (next.getX() == worldPosition.getX() && next.getZ() == worldPosition.getZ() && !hasPipe()) return false;
        var state = level.getBlockState(next);
        if (!canMine(next, state, tool, drill)) { blocked = true; return false; }
        if (!next.equals(working)) {
            if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, next, state, FakePlayerFactory.getMinecraft((ServerLevel) level))).isCanceled()) { blocked = true; return false; }
            working = next.immutable(); progress = 0; setChanged(); return true;
        }
        blocked = false;
        int cost = Math.clamp(drill.machineEuPerTick(tool), 1, 10000);
        if (energy < cost) return false;
        energy -= cost; progress += Math.clamp(drill.progressPerTick(tool, state), 1, 200); setChanged();
        if (progress >= 200) complete(tool, drill, state);
        return true;
    }
    private BlockPos obstruction(BlockPos target, BlockPos start) {
        var cursor = start;
        while (cursor.getX() != target.getX() || cursor.getZ() != target.getZ()) {
            int dx = target.getX() - cursor.getX(), dz = target.getZ() - cursor.getZ();
            cursor = cursor.relative(Math.abs(dx) > Math.abs(dz) ? dx > 0 ? Direction.EAST : Direction.WEST : dz > 0 ? Direction.SOUTH : Direction.NORTH);
            if (!level.hasChunkAt(cursor)) return cursor;
            var state = level.getBlockState(cursor);
            if (!state.isAir() && !(state.getBlock() instanceof MiningPipeBlock) && (state.getFluidState().isEmpty() || state.getFluidState().isSource())) return cursor;
        }
        return target;
    }
    private boolean canMine(BlockPos pos, BlockState state, ItemStack tool, MiningDrill drill) {
        if (state.getBlock() instanceof MiningPipeBlock || state.getDestroySpeed(level, pos) < 0) return false;
        if (!state.getFluidState().isEmpty() && state.getFluidState().isSource()) return pump(state, true);
        return state.isAir() || !state.getFluidState().isEmpty() || drill.canMine(tool, state, level, pos);
    }
    private boolean pump(BlockState state, boolean simulate) {
        var fluid = new FluidStack(state.getFluidState().getType(), 1000);
        for (var side : Direction.values()) {
            var pos = worldPosition.relative(side);
            if (level.hasChunkAt(pos) && level.getBlockEntity(pos) instanceof ic2.api.machine.FluidPump pump && pump.pumpFluid(fluid, simulate)) return true;
        }
        return false;
    }
    private void complete(ItemStack tool, MiningDrill drill, BlockState state) {
        var server = (ServerLevel) level;
        var player = FakePlayerFactory.getMinecraft(server);
        if (NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(level, working, state, player)).isCanceled()) { blocked = true; progress = 0; working = null; return; }
        if (!state.getFluidState().isEmpty() && state.getFluidState().isSource()) {
            if (!(state.getBlock() instanceof BucketPickup pickup) || !pump(state, true) || pickup.pickupBlock(player, level, working, state).isEmpty()) { blocked = true; return; }
            if (!pump(state, false)) { level.setBlock(working, state, 2 | 16); blocked = true; return; }
        } else {
            for (var drop : Block.getDrops(state, server, working, level.getBlockEntity(working), player, tool)) send(drop);
            level.setBlock(working, Blocks.AIR.defaultBlockState(), 2 | 16);
        }
        drill.consumeUse(tool);
        if (working.getX() == worldPosition.getX() && working.getZ() == worldPosition.getZ()) {
            if (!hasPipe()) { blocked = true; return; }
            MiningWorld.install(level, working);
            inventory.extractItem(2, 1, false); energy -= 10;
        }
        if (working.equals(desired)) desired = null;
        working = null; progress = 0; setChanged();
    }
    private void scan(BlockPos start, ItemStack tool, MiningDrill drill) {
        var stack = inventory.getStackInSlot(1);
        if (!(stack.getItem() instanceof WorldScanner scanner) || scanner.scansFluids()) return;
        int radius = Math.clamp(scanner.scanRadius(stack, true), 0, 32);
        if (radius == 0) return;
        var queue = new ArrayDeque<BlockPos>(); var visited = new HashSet<BlockPos>(); queue.add(start); visited.add(start);
        while (!queue.isEmpty()) {
            var pos = queue.removeFirst();
            if (!level.hasChunkAt(pos) || Math.abs(pos.getX() - worldPosition.getX()) > radius || Math.abs(pos.getZ() - worldPosition.getZ()) > radius) continue;
            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof MiningPipeBlock) && !canMine(pos, state, tool, drill)) continue;
            if (!(state.getBlock() instanceof MiningPipeBlock) && scanner.isValuable(stack, state, level, pos)) targets.add(pos);
            for (var side : Direction.Plane.HORIZONTAL) { var next = pos.relative(side); if (visited.add(next)) queue.add(next); }
        }
        setChanged();
    }
    private void retract() { MiningWorld.retract(level, worldPosition, inventory, 2); }
    private void send(ItemStack stack) { MiningWorld.send(level, worldPosition, stack); }
    IItemHandler automation(Direction side) { return !live() || side == Direction.DOWN || side == Direction.UP ? null : GuardedCapabilities.items(() -> live()
        ? new net.neoforged.neoforge.items.wrapper.CombinedInvWrapper(new BatterySlot(inventory, 0, false), new net.neoforged.neoforge.items.wrapper.RangedWrapper(inventory, 1, 4)) : null); }
    @Override public boolean canReceiveEnergy(Direction side, int output, int packet) { return live() && packet > 0 && packet <= 128 && energy < 10000; }
    @Override public int receiveEnergy(int amount) { if (!live()) return 0; int received = Math.clamp(amount, 0, 10000 - energy); energy += received; if (received > 0) setChanged(); return received; }
    @Override public MachineSnapshot machineInfo() { return MachineTelemetry.snapshot(this, energy, 10000, MachineSnapshot.EnergyUnit.EU,
        java.util.Map.of("progress_units", (long) progress, "progress_target", 200L, "input_packet_eu", 128L, "blocked", blocked ? 1L : 0L, "queued_targets", (long) targets.size(), "pipe_tip_y", (long) tip().getY())); }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory player, Player owner) { return new ExtractionMenu(ExtractionMachines.MINER_MENU.get(), id, player, this, inventory, data, -1); }
    @Override public void dropContents() { for (int slot = 0; slot < inventory.getSlots(); slot++) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), inventory.getStackInSlot(slot)); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup); tag.putInt("energy", energy); tag.putInt("progress", progress); tag.putBoolean("blocked", blocked);
        tag.put("inventory", inventory.serializeNBT(lookup)); if (desired != null) tag.putLong("desired", desired.asLong()); if (working != null) tag.putLong("working", working.asLong());
        var queue = new ListTag(); for (var pos : targets) { var entry = new CompoundTag(); entry.putLong("position", pos.asLong()); queue.add(entry); } tag.put("targets", queue);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup); energy = Math.clamp(tag.getInt("energy"), 0, 10000); progress = Math.clamp(tag.getInt("progress"), 0, 200); blocked = tag.getBoolean("blocked");
        inventory.deserializeNBT(lookup, tag.getCompound("inventory")); desired = tag.contains("desired") ? BlockPos.of(tag.getLong("desired")) : null; working = tag.contains("working") ? BlockPos.of(tag.getLong("working")) : null;
        targets.clear(); for (var entry : tag.getList("targets", 10)) { if (targets.size() >= 4096) break; targets.add(BlockPos.of(((CompoundTag) entry).getLong("position"))); }
    }
}
