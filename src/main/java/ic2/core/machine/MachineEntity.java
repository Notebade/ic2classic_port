package ic2.core.machine;

import ic2.core.IC2;
import ic2.core.item.ReBatteryItem;
import java.util.Optional;
import java.util.function.Predicate;
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
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

final class MachineEntity extends BlockEntity implements MenuProvider {
    static final int INPUT_SLOT = 0;
    static final int OUTPUT_SLOT = 1;
    static final int SLOT_COUNT = 2;
    private static final int GENERATOR_OUTPUT = 10;
    private static final int BATBOX_OUTPUT = 32;
    private static final int STACK_LIMIT = 64;

    private final MachineKind kind;
    private final PumpTank pumpTank;
    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> smelting =
        RecipeManager.createCheck(RecipeType.SMELTING);
    private int energy;
    private int burnTicks;
    private int progress;
    private int amplifier;
    private int amplifierRefillThreshold;
    private long nextPumpAttempt;
    private ItemStack processingInput = ItemStack.EMPTY;
    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (kind == MachineKind.BATBOX) {
                return ReBatteryItem.isBattery(stack);
            }
            return slot == INPUT_SLOT && accepts(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == INPUT_SLOT && kind.recipeTicks > 0 && kind != MachineKind.PUMP) {
                trackInput();
            }
            setChanged();
        }
    };
    private final IItemHandler automationInput = new RangedWrapper(inventory, INPUT_SLOT, INPUT_SLOT + 1) {
        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }
    };
    private final IItemHandler automationOutput = new RangedWrapper(inventory, OUTPUT_SLOT, OUTPUT_SLOT + 1);
    private final IItemHandler batteryChargeSlot = new BatterySlot(inventory, INPUT_SLOT, true);
    private final IItemHandler batteryDischargeSlot = new BatterySlot(inventory, OUTPUT_SLOT, false);

    MachineEntity(BlockPos pos, BlockState state) {
        super(typeFor(state), pos, state);
        kind = ((MachineBlock) state.getBlock()).kind();
        pumpTank = kind == MachineKind.PUMP ? new PumpTank(this::setChanged) : null;
    }

    static BlockEntityType<MachineEntity> typeFor(BlockState state) {
        return switch (((MachineBlock) state.getBlock()).kind()) {
            case GENERATOR -> Machines.GENERATOR_ENTITY.get();
            case SOLAR_PANEL -> Machines.SOLAR_PANEL_ENTITY.get();
            case MACERATOR -> Machines.MACERATOR_ENTITY.get();
            case ELECTRIC_FURNACE -> Machines.ELECTRIC_FURNACE_ENTITY.get();
            case BATBOX -> Machines.BATBOX_ENTITY.get();
            case EXTRACTOR -> Machines.EXTRACTOR_ENTITY.get();
            case COMPRESSOR -> Machines.COMPRESSOR_ENTITY.get();
            case PUMP -> Machines.PUMP_ENTITY.get();
            case RECYCLER -> Machines.RECYCLER_ENTITY.get();
            case MASS_FABRICATOR -> Machines.MASS_FABRICATOR_ENTITY.get();
        };
    }

    void tick() {
        switch (kind) {
            case GENERATOR -> tickGenerator();
            case SOLAR_PANEL -> tickSolarPanel();
            case PUMP -> tickPump();
            case MASS_FABRICATOR -> tickMassFabricator();
            case MACERATOR, ELECTRIC_FURNACE, EXTRACTOR, COMPRESSOR, RECYCLER -> tickProcessor();
            case BATBOX -> tickStorage();
        }
    }

    private void tickMassFabricator() {
        if (energy >= MassFabrication.MATTER_COST) {
            ItemStack matter = new ItemStack(IC2.UU_MATTER.get());
            if (canAddOutput(matter)) {
                energy -= MassFabrication.MATTER_COST;
                addOutput(matter);
            }
        }
        refillAmplifier();
    }

    private void refillAmplifier() {
        if (amplifier != 0 && amplifier >= amplifierRefillThreshold) {
            return;
        }
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        int value = MassFabrication.amplifierValue(input);
        if (value > 0) {
            int count = Math.min(MassFabrication.REFILL_BATCH, input.getCount());
            amplifier += count * value;
            amplifierRefillThreshold = MassFabrication.REFILL_BATCH * value;
            inventory.extractItem(INPUT_SLOT, count, false);
        }
    }

    private void tickPump() {
        if (progress < kind.recipeTicks && energy >= kind.energyPerTick) {
            energy -= kind.energyPerTick;
            progress++;
            setChanged();
        }
        if (progress == kind.recipeTicks && level.getGameTime() >= nextPumpAttempt
            && pumpTank.getFluidAmount() <= pumpTank.getCapacity() - PumpTank.BUCKET) {
            if (pumpTank.collect(level, worldPosition.below())) {
                progress = 0;
                setChanged();
            } else {
                nextPumpAttempt = level.getGameTime() + 20;
            }
        }
        fillPumpBucket();
    }

    private void fillPumpBucket() {
        if (pumpTank.getFluidAmount() < PumpTank.BUCKET || !inventory.getStackInSlot(INPUT_SLOT).is(Items.BUCKET)) {
            return;
        }
        ItemStack filled = new ItemStack(pumpTank.getFluid().getFluid().getBucket());
        if (!filled.isEmpty() && canAddOutput(filled)) {
            pumpTank.drain(PumpTank.BUCKET, FluidAction.EXECUTE);
            inventory.extractItem(INPUT_SLOT, 1, false);
            addOutput(filled);
        }
    }

    private void tickStorage() {
        transferBattery(INPUT_SLOT, true);
        transferBattery(OUTPUT_SLOT, false);
        if (energy >= BATBOX_OUTPUT) {
            sendEnergy(BATBOX_OUTPUT, outputSide()::equals, false);
        }
    }

    private void tickSolarPanel() {
        boolean active = SolarPanelBlock.hasSun(level, worldPosition);
        BlockState state = getBlockState();
        if (state.getValue(BlockStateProperties.LIT) != active) {
            level.setBlock(worldPosition, state.setValue(BlockStateProperties.LIT, active), Block.UPDATE_CLIENTS);
        }
        if (active) {
            CopperNetwork.send(level, worldPosition, SolarPanelBlock.OUTPUT, side -> side != Direction.UP, true);
        }
    }

    private void transferBattery(int slot, boolean charging) {
        ItemStack battery = inventory.getStackInSlot(slot);
        if (!ReBatteryItem.isBattery(battery)) {
            return;
        }
        int charge = ReBatteryItem.charge(battery);
        int available = charging
            ? Math.min(energy, ReBatteryItem.CAPACITY - charge)
            : Math.min(kind.capacity - energy, charge);
        int moved = Math.min(ReBatteryItem.TRANSFER_PER_TICK, available);
        if (moved > 0) {
            int batteryGain = charging ? moved : -moved;
            ItemStack updated = battery.copy();
            ReBatteryItem.setCharge(updated, charge + batteryGain);
            energy -= batteryGain;
            inventory.setStackInSlot(slot, updated);
        }
    }

    private void tickGenerator() {
        if (burnTicks == 0 && energy <= kind.capacity - GENERATOR_OUTPUT) {
            startBurning();
        }
        if (burnTicks > 0) {
            burnTicks--;
            energy = Math.min(kind.capacity, energy + GENERATOR_OUTPUT);
            setChanged();
        }
        if (energy > 0) {
            sendEnergy(Math.min(GENERATOR_OUTPUT, energy), side -> true, true);
        }
    }

    private void sendEnergy(int available, Predicate<Direction> emitsTo, boolean canChargeStorage) {
        int sent = CopperNetwork.send(level, worldPosition, available, emitsTo, canChargeStorage);
        if (sent > 0) {
            energy -= sent;
            setChanged();
        }
    }

    private void startBurning() {
        ItemStack fuel = inventory.getStackInSlot(INPUT_SLOT);
        int ticks = fuelTicks(fuel);
        if (ticks <= 0) {
            return;
        }
        ItemStack remainder = fuel.copyWithCount(1).getCraftingRemainingItem();
        if (!canAddOutput(remainder)) {
            return;
        }
        inventory.extractItem(INPUT_SLOT, 1, false);
        addOutput(remainder);
        burnTicks = ticks;
    }

    private boolean accepts(ItemStack stack) {
        return switch (kind) {
            case GENERATOR -> fuelTicks(stack) > 0;
            case MACERATOR -> stack.is(Items.RAW_COPPER);
            case ELECTRIC_FURNACE -> furnaceRecipe(new SingleRecipeInput(stack)).isPresent();
            case EXTRACTOR -> ExtractionRecipes.find(stack) != null;
            case COMPRESSOR -> CompressionRecipes.find(stack) != null;
            case PUMP -> stack.is(Items.BUCKET);
            case RECYCLER -> RecyclingRecipes.find(stack) != null;
            case MASS_FABRICATOR -> MassFabrication.amplifierValue(stack) > 0;
            case BATBOX, SOLAR_PANEL -> false;
        };
    }

    private Optional<RecipeHolder<SmeltingRecipe>> furnaceRecipe(SingleRecipeInput input) {
        return level == null || input.isEmpty() ? Optional.empty() : smelting.getRecipeFor(input, level);
    }

    private Work workFor(ItemStack stack) {
        return switch (kind) {
            case MACERATOR -> stack.is(Items.RAW_COPPER) ? new Work(1, new ItemStack(IC2.COPPER_DUST.get(), 2)) : null;
            case ELECTRIC_FURNACE -> new Work(1, smeltingResult(stack));
            case EXTRACTOR -> new Work(1, ExtractionRecipes.result(stack));
            case COMPRESSOR -> {
                CompressionRecipes.Recipe recipe = CompressionRecipes.find(stack);
                yield recipe == null ? null : new Work(recipe.inputCount(), new ItemStack(recipe.output()));
            }
            case RECYCLER -> {
                RecyclingRecipes.Recipe recipe = RecyclingRecipes.find(stack);
                yield recipe == null ? null
                    : new Work(recipe.inputCount(), new ItemStack(recipe.output(), recipe.count()),
                        recipe.chanceDenominator(), recipe.extraTicks());
            }
            case GENERATOR, BATBOX, SOLAR_PANEL, PUMP, MASS_FABRICATOR -> null;
        };
    }

    private ItemStack smeltingResult(ItemStack stack) {
        var input = new SingleRecipeInput(stack);
        return furnaceRecipe(input)
            .map(recipe -> recipe.value().assemble(input, level.registryAccess()))
            .orElse(ItemStack.EMPTY);
    }

    private void trackInput() {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        if (!ItemStack.isSameItemSameComponents(processingInput, input)) {
            processingInput = input.copyWithCount(1);
            progress = 0;
        }
    }

    private void tickProcessor() {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        PumpTank waterSource = kind == MachineKind.COMPRESSOR && input.isEmpty() ? findWaterSource() : null;
        Work work = waterSource == null ? workFor(input) : new Work(0, new ItemStack(Items.SNOWBALL));
        if (work == null || work.output().isEmpty() || input.getCount() < work.inputCount()) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return;
        }
        if (!canAddOutput(work.output()) || energy < kind.energyPerTick) {
            return;
        }
        energy -= kind.energyPerTick;
        progress++;
        if (progress >= kind.recipeTicks + work.extraTicks()) {
            if (waterSource == null) {
                inventory.extractItem(INPUT_SLOT, work.inputCount(), false);
            } else {
                waterSource.drain(new FluidStack(Fluids.WATER, PumpTank.BUCKET), FluidAction.EXECUTE);
            }
            if (work.chanceDenominator() == 1 || level.random.nextInt(work.chanceDenominator()) == 0) {
                addOutput(work.output());
            }
            progress = 0;
        }
        setChanged();
    }

    private record Work(int inputCount, ItemStack output, int chanceDenominator, int extraTicks) {
        Work(int inputCount, ItemStack output) {
            this(inputCount, output, 1, 0);
        }
    }

    private PumpTank findWaterSource() {
        for (Direction direction : Direction.values()) {
            BlockPos neighbor = worldPosition.relative(direction);
            if (level.isLoaded(neighbor) && level.getBlockEntity(neighbor) instanceof MachineEntity machine
                && machine.pumpTank != null && machine.pumpTank.getFluidAmount() >= PumpTank.BUCKET
                && machine.pumpTank.getFluid().getFluid() == Fluids.WATER) {
                return machine.pumpTank;
            }
        }
        return null;
    }

    private boolean canAddOutput(ItemStack addition) {
        ItemStack current = inventory.getStackInSlot(OUTPUT_SLOT);
        return addition.isEmpty() || (current.isEmpty() && addition.getCount() <= addition.getMaxStackSize())
            || (ItemStack.isSameItemSameComponents(current, addition)
                && current.getCount() + addition.getCount() <= current.getMaxStackSize());
    }

    private void addOutput(ItemStack addition) {
        if (!addition.isEmpty()) {
            int count = inventory.getStackInSlot(OUTPUT_SLOT).getCount() + addition.getCount();
            inventory.setStackInSlot(OUTPUT_SLOT, addition.copyWithCount(count));
        }
    }

    boolean canReceiveEnergy(Direction incomingSide, boolean canChargeStorage) {
        if (energy >= kind.capacity) {
            return false;
        }
        return switch (kind) {
            case GENERATOR, SOLAR_PANEL -> false;
            case MACERATOR, ELECTRIC_FURNACE, EXTRACTOR, COMPRESSOR, PUMP, RECYCLER -> true;
            case MASS_FABRICATOR -> !level.hasNeighborSignal(worldPosition);
            case BATBOX -> canChargeStorage && incomingSide != outputSide();
        };
    }

    private Direction outputSide() {
        return getBlockState().getValue(BlockStateProperties.FACING);
    }

    Component storageStatus() {
        return Component.translatable("message.ic2.batbox_status", energy, kind.capacity, BATBOX_OUTPUT,
            Component.translatable("direction.ic2." + outputSide().getName()));
    }

    int receiveEnergy(int available) {
        int received = Math.min(available, kind.capacity - energy);
        if (kind == MachineKind.MASS_FABRICATOR) {
            received = Math.min(received, MassFabrication.MAX_INPUT);
            int bonusBudget = (kind.capacity - energy - received) / MassFabrication.BONUS;
            int boosted = Math.min(Math.min(amplifier, received), bonusBudget);
            amplifier -= boosted;
            energy += boosted * MassFabrication.BONUS;
        }
        energy += received;
        setChanged();
        return received;
    }

    private static int fuelTicks(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) / 4;
    }

    IItemHandler automation(Direction side) {
        if (kind == MachineKind.PUMP) {
            if (side == Direction.UP) {
                return automationInput;
            }
            return side != null && side.getAxis().isHorizontal() ? automationOutput : null;
        }
        if (kind == MachineKind.BATBOX) {
            if (side == Direction.UP) {
                return batteryChargeSlot;
            }
            return side == Direction.DOWN ? batteryDischargeSlot : null;
        }
        return side == Direction.DOWN ? automationOutput : automationInput;
    }

    IFluidHandler fluidOutput(Direction side) {
        return side == Direction.DOWN ? null : pumpTank;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new MachineMenu(id, playerInventory, inventory, ContainerLevelAccess.create(level, worldPosition),
            getBlockState().getBlock(), () -> energy,
            this::menuWork, () -> pumpTank == null ? -1 : pumpTank.getFluidAmount(),
            this::setChanged, switch (kind) {
                case BATBOX -> Machines.BATBOX_MENU.get();
                case MASS_FABRICATOR -> Machines.FABRICATOR_MENU.get();
                default -> Machines.MENU.get();
            });
    }

    private int menuWork() {
        return switch (kind) {
            case GENERATOR -> burnTicks;
            case BATBOX -> outputSide().ordinal();
            case SOLAR_PANEL -> 0;
            case MASS_FABRICATOR -> amplifier;
            case MACERATOR, ELECTRIC_FURNACE, EXTRACTOR, COMPRESSOR, PUMP, RECYCLER -> progress;
        };
    }

    void dropContents(Level world) {
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            Containers.dropItemStack(world, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(),
                inventory.getStackInSlot(slot));
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (kind == MachineKind.SOLAR_PANEL) {
            return;
        }
        tag.putInt("energy", energy);
        tag.put("inventory", inventory.serializeNBT(registries));
        if (pumpTank != null) {
            tag.put("tank", pumpTank.writeToNBT(registries, new CompoundTag()));
        }
        if (kind == MachineKind.GENERATOR) {
            tag.putInt("burn_ticks", burnTicks);
        } else if (kind == MachineKind.MASS_FABRICATOR) {
            tag.putInt("amplifier", amplifier);
            tag.putInt("amplifier_refill", amplifierRefillThreshold);
        } else if (kind.recipeTicks > 0) {
            tag.putInt("progress", progress);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (kind == MachineKind.SOLAR_PANEL) {
            return;
        }
        energy = Mth.clamp(tag.getInt("energy"), 0, kind.capacity);
        if (pumpTank != null) {
            pumpTank.readFromNBT(registries, tag.getCompound("tank"));
        }
        if (tag.contains("inventory", CompoundTag.TAG_COMPOUND)) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        } else if (kind == MachineKind.MACERATOR) {
            inventory.setStackInSlot(INPUT_SLOT, new ItemStack(Items.RAW_COPPER, Mth.clamp(tag.getInt("input"), 0, STACK_LIMIT)));
            inventory.setStackInSlot(OUTPUT_SLOT, new ItemStack(IC2.COPPER_DUST.get(), Mth.clamp(tag.getInt("output"), 0, STACK_LIMIT)));
        }
        if (kind == MachineKind.GENERATOR) {
            burnTicks = Math.max(0, tag.getInt("burn_ticks"));
        } else if (kind == MachineKind.MASS_FABRICATOR) {
            amplifier = Mth.clamp(tag.getInt("amplifier"), 0, 2_000_000);
            amplifierRefillThreshold = Mth.clamp(tag.getInt("amplifier_refill"), 0, 1_000_000);
        } else if (kind.recipeTicks > 0) {
            processingInput = inventory.getStackInSlot(INPUT_SLOT).copyWithCount(1);
            int maxProgress = kind == MachineKind.PUMP ? kind.recipeTicks : kind.recipeTicks - 1;
            if (kind == MachineKind.RECYCLER && processingInput.is(IC2.SCRAP.get())) {
                maxProgress += RecyclingRecipes.BOX_EXTRA_TICKS;
            }
            progress = Mth.clamp(tag.getInt("progress"), 0, maxProgress);
        }
    }
}
