package ic2.core.machine;

import ic2.api.machine.MachineSnapshot;
import ic2.core.IC2;
import ic2.core.item.FluidCellItem;
import ic2.core.item.RechargeableItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

final class ExtractionEntity extends BlockEntity implements EnergySink, MachineSnapshot.Provider, ExtractionDevice, ic2.api.machine.FluidPump {
    private int energy, progress, delay;
    private final PumpTank tank = new PumpTank(this::setChanged);
    final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 ? RechargeableItem.supports(stack, 128, false)
                : slot == 1 && (stack.is(Items.BUCKET) || stack.is(IC2.CELL_EMPTY.get()));
        }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final RangedWrapper output = new RangedWrapper(inventory, 2, 3);
    final ContainerData data = new ContainerData() {
        @Override public int getCount() { return 4; }
        @Override public int get(int index) { return switch (index) { case 0 -> energy; case 1 -> progress; case 2 -> tank.getFluidAmount(); case 3 -> 2500; default -> 0; }; }
        @Override public void set(int index, int value) {}
    };
    ExtractionEntity(BlockPos pos, BlockState state) { super(ExtractionMachines.ENTITY.get(), pos, state); }
    private boolean live() { return level != null && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this; }
    @Override public void tick() {
        if (!live()) return;
        dischargeBattery();
        if (delay > 0) delay--;
        if (progress < 25 && energy >= 8) { energy -= 8; progress++; setChanged(); }
        if (progress >= 25 && delay == 0 && tank.getFluidAmount() <= 3000) {
            if (level.hasChunkAt(worldPosition.below()) && tank.collect(level, worldPosition.below())) { progress = 0; setChanged(); }
            else delay = 20;
        }
        fillContainer();
        exportFluids();
        boolean lit = progress >= 25 && energy > 0;
        if (getBlockState().getValue(BlockStateProperties.LIT) != lit)
            level.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.LIT, lit), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
    private void dischargeBattery() {
        var stack = inventory.getStackInSlot(0);
        if (RechargeableItem.supports(stack, 128, false)) {
            var battery = (RechargeableItem) stack.getItem();
            int amount = Math.min(2250 - energy, Math.min(battery.transferRate(), battery.charge(stack)));
            if (amount > 0) { battery.setCharge(stack, battery.charge(stack) - amount); energy += amount; setChanged(); }
        }
    }
    private void fillContainer() {
        if (tank.getFluidAmount() < 1000) return;
        var input = inventory.getStackInSlot(1);
        var fluid = tank.getFluid().getFluid();
        var result = input.is(Items.BUCKET) ? new ItemStack(fluid.getBucket())
            : input.is(IC2.CELL_EMPTY.get()) ? FluidCellItem.filledStack(fluid) : ItemStack.EMPTY;
        var current = inventory.getStackInSlot(2);
        if (result.isEmpty() || !current.isEmpty() && (!ItemStack.isSameItemSameComponents(current, result) || current.getCount() + result.getCount() > result.getMaxStackSize())) return;
        tank.drain(1000, IFluidHandler.FluidAction.EXECUTE);
        inventory.extractItem(1, 1, false);
        inventory.setStackInSlot(2, result.copyWithCount(current.getCount() + result.getCount()));
    }
    private void exportFluids() {
        if (tank.isEmpty()) return;
        for (var direction : Direction.values()) {
            if (direction == Direction.DOWN || !level.hasChunkAt(worldPosition.relative(direction))) continue;
            var other = level.getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(direction), direction.getOpposite());
            if (other != null) FluidUtil.tryFluidTransfer(other, tank, 100, true);
            if (tank.isEmpty()) return;
        }
    }
    IItemHandler automation(Direction side) {
        if (side == Direction.DOWN || !live()) return null;
        return GuardedCapabilities.items(() -> !live() ? null : side == Direction.UP ? new RangedWrapper(inventory, 1, 2)
            : new CombinedInvWrapper(new BatterySlot(inventory, 0, false), output));
    }
    IFluidHandler fluidOutput(Direction side) { return side == Direction.DOWN || !live() ? null : GuardedCapabilities.fluids(() -> live() ? tank : null); }
    @Override public boolean pumpFluid(net.neoforged.neoforge.fluids.FluidStack fluid, boolean simulate) {
        if (!live() || progress < 25 || !tank.acceptCollected(fluid, simulate)) return false;
        if (!simulate) { progress = 0; setChanged(); }
        return true;
    }
    @Override public boolean canReceiveEnergy(Direction side, int storageOutput, int packet) { return live() && packet > 0 && packet <= 128 && energy < 2500; }
    @Override public int receiveEnergy(int available) { if (!live()) return 0; int accepted = Math.clamp(available, 0, 2500 - energy); energy += accepted; if (accepted > 0) setChanged(); return accepted; }
    @Override public MachineSnapshot machineInfo() { return MachineTelemetry.snapshot(this, energy, 2500, MachineSnapshot.EnergyUnit.EU, java.util.Map.of("input_packet_eu", 128L, "progress_units", (long) progress, "progress_target", 25L, "fluid_mb", (long) tank.getFluidAmount())); }
    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory player, Player owner) { return new ExtractionMenu(id, player, this); }
    @Override public void dropContents() { for (int slot = 0; slot < inventory.getSlots(); slot++) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), inventory.getStackInSlot(slot)); }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup); tag.putInt("energy", energy); tag.putInt("progress", progress); tag.putInt("delay", delay);
        tag.put("inventory", inventory.serializeNBT(lookup)); tag.put("tank", tank.writeToNBT(lookup, new CompoundTag()));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup); energy = Math.clamp(tag.getInt("energy"), 0, 2500); progress = Math.clamp(tag.getInt("progress"), 0, 25); delay = Math.clamp(tag.getInt("delay"), 0, 20);
        inventory.deserializeNBT(lookup, tag.getCompound("inventory")); tank.readFromNBT(lookup, tag.getCompound("tank"));
    }
}
