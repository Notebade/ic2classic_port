package ic2.core.machine;

import ic2.api.machine.MachineSnapshot;

import ic2.core.IC2;
import ic2.core.item.RechargeableItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

final class ExtraGeneratorEntity extends BlockEntity implements MenuProvider, MachineSnapshot.Provider {
    @Override public MachineSnapshot machineInfo() {
        return MachineTelemetry.snapshot(this, energy, type.maxStored(), MachineSnapshot.EnergyUnit.EU,
            type == ExtraGenerators.Type.SOLAR ? java.util.Map.of("heat", (long) solar.heat(), "water_units", (long) solar.water(), "output_eu_hundredths", (long) outputHundredths)
                : java.util.Map.of("fuel_ticks", (long) remainingFuel, "output_eu_hundredths", (long) outputHundredths));
    }
    final ExtraGenerators.Type type = ((ExtraGeneratorBlock) getBlockState().getBlock()).type;
    private SolarTurbineCycle.State solar = new SolarTurbineCycle.State(0, 0, 0, 0);
    private int energy;
    private int remainingFuel;
    private int maxFuel;
    private int scrapCounter = -1;
    private boolean scrapFuel;
    private boolean sunny;
    private int sunAge = 128;
    private int outputHundredths;
    final ItemStackHandler inventory = new ItemStackHandler(3) {
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 ? RechargeableItem.supports(stack, type.chargeTier)
                : slot == 1 && (type == ExtraGenerators.Type.SOLAR
                    ? FluidUtil.getFluidContained(stack).filter(fluid -> fluid.is(Fluids.WATER)).isPresent() : rawFuel(stack) > 0);
        }
        @Override protected void onContentsChanged(int slot) { setChanged(); }
    };
    private final BatterySlot battery = new BatterySlot(inventory, 0, true);
    private final RangedWrapper output = new RangedWrapper(inventory, 2, 3);
    private final RangedWrapper input = new RangedWrapper(inventory, 1, 2) {
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return type == ExtraGenerators.Type.SLAG && rawFuel(getStackInSlot(slot)) == 0
                ? super.extractItem(slot, amount, simulate) : ItemStack.EMPTY;
        }
    };
    private final IItemHandler sides = new CombinedInvWrapper(battery, input, output);
    private final IItemHandler belowSolar = new CombinedInvWrapper(battery, output);
    private final IItemHandler aboveSlag = input;
    private final IItemHandler sidesSlag = new CombinedInvWrapper(input, output);
    final IFluidHandler waterHandler = new IFluidHandler() {
        @Override public int getTanks() { return 1; }
        @Override public FluidStack getFluidInTank(int tank) { return new FluidStack(Fluids.WATER, solar.water()); }
        @Override public int getTankCapacity(int tank) { return SolarTurbineCycle.MAX_WATER; }
        @Override public boolean isFluidValid(int tank, FluidStack stack) { return stack.is(Fluids.WATER); }
        @Override public int fill(FluidStack resource, FluidAction action) {
            if (!resource.is(Fluids.WATER)) return 0;
            var fill = solar.fill(resource.getAmount());
            if (action.execute() && fill.accepted() > 0) { solar = fill.next(); setChanged(); }
            return fill.accepted();
        }
        @Override public FluidStack drain(FluidStack resource, FluidAction action) { return FluidStack.EMPTY; }
        @Override public FluidStack drain(int amount, FluidAction action) { return FluidStack.EMPTY; }
    };
    final ContainerData data = new ContainerData() {
        @Override public int getCount() { return 6; }
        @Override public void set(int index, int value) {}
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> energy;
                case 1 -> solar.heat();
                case 2 -> solar.water() & 0xffff;
                case 3 -> solar.water() >>> 16;
                case 4 -> maxFuel == 0 ? 0 : (int) (1000L * remainingFuel / maxFuel);
                case 5 -> outputHundredths;
                default -> throw new IndexOutOfBoundsException(index);
            };
        }
    };

    ExtraGeneratorEntity(BlockPos pos, BlockState state) { super(ExtraGenerators.ENTITY.get(), pos, state); }

    void tick() {
        boolean active = type == ExtraGenerators.Type.SOLAR ? tickSolar() : tickSlag();
        chargeBattery();
        int sent = EnergyNetwork.send(level, worldPosition, Math.min(energy, type.packet), side -> true, 0);
        if (sent > 0) { energy -= sent; setChanged(); }
        BlockState state = getBlockState();
        if (state.getValue(BlockStateProperties.LIT) != active) level.setBlock(worldPosition, state.setValue(BlockStateProperties.LIT, active), Block.UPDATE_CLIENTS);
    }

    private boolean tickSolar() {
        refillWater();
        if (++sunAge >= 128) { sunny = SolarPanelBlock.hasSun(level, worldPosition); sunAge = 0; }
        var step = solar.step(energy, sunny);
        if (!solar.equals(step.next()) || energy != step.energy()) setChanged();
        solar = step.next(); energy = step.energy(); outputHundredths = step.outputHundredths();
        return sunny;
    }

    private void refillWater() {
        ItemStack stack = inventory.getStackInSlot(1);
        if (stack.isEmpty() || solar.water() >= 38000) return;
        var preview = FluidUtil.tryEmptyContainer(stack, waterHandler, Integer.MAX_VALUE, null, false);
        if (preview.isSuccess() && canOutput(preview.getResult())) {
            var drained = FluidUtil.tryEmptyContainer(stack, waterHandler, Integer.MAX_VALUE, null, true);
            if (drained.isSuccess()) { inventory.extractItem(1, 1, false); addOutput(drained.getResult()); }
        }
    }

    private boolean tickSlag() {
        if (scrapCounter < 1) scrapCounter = nextScrapCounter();
        if (remainingFuel <= 0 && SlagGeneratorCycle.canRefuel(energy)) startFuel();
        var step = SlagGeneratorCycle.step(remainingFuel, energy, scrapCounter, scrapFuel);
        remainingFuel = step.remaining(); energy = step.energy(); scrapCounter = step.counter();
        outputHundredths = step.burning() ? SlagGeneratorCycle.OUTPUT * 100 : 0;
        if (step.scrap()) {
            scrapCounter = nextScrapCounter();
            ItemStack scrap = new ItemStack(IC2.SCRAP.get());
            if (canOutput(scrap)) addOutput(scrap);
        }
        if (step.burning()) setChanged();
        return step.burning();
    }

    private int nextScrapCounter() { return 80 + level.random.nextInt(160); }

    static int rawFuel(ItemStack stack) {
        if (stack.isEmpty() || FluidUtil.getFluidContained(stack).filter(fluid -> fluid.is(Fluids.LAVA)).isPresent()) return 0;
        return stack.getBurnTime(RecipeType.SMELTING);
    }

    private void startFuel() {
        ItemStack stack = inventory.getStackInSlot(1);
        int raw = rawFuel(stack);
        if (raw <= 0) return;
        ItemStack remainder = stack.getCraftingRemainingItem();
        if (stack.getCount() > 1 && !canOutput(remainder)) return;
        scrapFuel = stack.is(IC2.SCRAP.get()) || stack.is(IC2.SCRAP_BOX.get());
        remainingFuel = SlagGeneratorCycle.fuelTicks(raw, scrapFuel); maxFuel = remainingFuel;
        inventory.extractItem(1, 1, false);
        if (!remainder.isEmpty()) {
            if (inventory.getStackInSlot(1).isEmpty()) inventory.setStackInSlot(1, remainder);
            else addOutput(remainder);
        }
        setChanged();
    }

    private boolean canOutput(ItemStack stack) {
        ItemStack current = inventory.getStackInSlot(2);
        return stack.isEmpty() || current.isEmpty() || ItemStack.isSameItemSameComponents(current, stack)
            && current.getCount() + stack.getCount() <= Math.min(current.getMaxStackSize(), inventory.getSlotLimit(2));
    }

    private void addOutput(ItemStack stack) {
        if (!stack.isEmpty()) inventory.setStackInSlot(2, stack.copyWithCount(inventory.getStackInSlot(2).getCount() + stack.getCount()));
    }

    private void chargeBattery() {
        ItemStack stack = inventory.getStackInSlot(0);
        if (!RechargeableItem.supports(stack, type.chargeTier)) return;
        RechargeableItem cell = (RechargeableItem) stack.getItem();
        int charge = cell.charge(stack);
        int moved = Math.min(energy, Math.min(cell.transferRate(), cell.capacity() - charge));
        if (moved > 0) {
            ItemStack updated = stack.copy(); cell.setCharge(updated, charge + moved);
            energy -= moved; inventory.setStackInSlot(0, updated);
        }
    }

    IItemHandler automation(Direction side) {
        if (side == null) return sides;
        if (type == ExtraGenerators.Type.SOLAR) return side == Direction.UP ? null : side == Direction.DOWN ? belowSolar : sides;
        return side == Direction.UP ? aboveSlag : side == Direction.DOWN ? sides : sidesSlag;
    }

    void dropContents() {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), inventory.getStackInSlot(slot));
        }
    }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory player, Player owner) {
        return new ExtraGeneratorMenu(id, player, type, inventory, data, ContainerLevelAccess.create(level, worldPosition), this);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy); tag.put("inventory", inventory.serializeNBT(registries));
        tag.putInt("heat", solar.heat()); tag.putInt("water", solar.water());
        tag.putInt("water_remainder", solar.waterRemainder()); tag.putInt("energy_remainder", solar.energyRemainder());
        tag.putInt("fuel", remainingFuel); tag.putInt("max_fuel", maxFuel);
        tag.putInt("scrap_counter", scrapCounter); tag.putBoolean("scrap_fuel", scrapFuel);
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy = Mth.clamp(tag.getInt("energy"), 0, type.maxStored());
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        solar = new SolarTurbineCycle.State(Mth.clamp(tag.getInt("heat"), 0, SolarTurbineCycle.MAX_HEAT),
            Mth.clamp(tag.getInt("water"), 0, SolarTurbineCycle.MAX_WATER), Mth.clamp(tag.getInt("water_remainder"), 0, 5999),
            Mth.clamp(tag.getInt("energy_remainder"), 0, SolarTurbineCycle.ENERGY_DENOMINATOR - 1));
        maxFuel = Math.max(0, tag.getInt("max_fuel")); remainingFuel = Mth.clamp(tag.getInt("fuel"), 0, maxFuel);
        scrapCounter = tag.contains("scrap_counter") ? Mth.clamp(tag.getInt("scrap_counter"), 1, 239) : -1;
        scrapFuel = tag.getBoolean("scrap_fuel"); sunAge = 128;
    }
}
