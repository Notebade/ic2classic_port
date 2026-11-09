package ic2.core.machine;

import ic2.core.IC2;
import ic2.core.item.FluidCellItem;
import ic2.core.item.RechargeableItem;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

final class MachineEntity extends BlockEntity implements MenuProvider {
    static final int INPUT_SLOT = 0;
    static final int OUTPUT_SLOT = 1;
    static final int SLOT_COUNT = 2;
    static final int GEOTHERMAL_BATTERY_SLOT = SLOT_COUNT;
    static final int SECOND_INPUT_SLOT = SLOT_COUNT;
    static final int GENERATOR_OUTPUT = 10;
    private static final int STACK_LIMIT = 64;

    private final MachineKind kind;
    private final PumpTank pumpTank;
    private final FluidTank geothermalTank;
    private final RecipeManager.CachedCheck<SingleRecipeInput, ? extends AbstractCookingRecipe> cooking;
    private final SawmillRecipes sawmill;
    private final RecipeManager.CachedCheck<SingleRecipeInput, MacerationRecipe> macerating =
        RecipeManager.createCheck(Machines.MACERATING.get());
    private int energy;
    private boolean electrolysisDischarging;
    private int burnTicks;
    private int windProduction = -1;
    private int windRemainder;
    private int progress;
    private int amplifier;
    private int amplifierRefillThreshold;
    private RareEarthRecipes.Material accumulatedMaterial;
    private int materialUnits;
    private long nextPumpAttempt;
    private ItemStack processingInput = ItemStack.EMPTY;
    private ItemStack processingSecondInput = ItemStack.EMPTY;
    private final ItemStackHandler inventory;
    private final IItemHandler automationInput;
    private final IItemHandler automationOutput;
    private final IItemHandler batteryChargeSlot;
    private final IItemHandler batteryDischargeSlot;

    MachineEntity(BlockPos pos, BlockState state) {
        super(typeFor(state), pos, state);
        kind = ((MachineBlock) state.getBlock()).kind();
        sawmill = kind == MachineKind.SAWMILL ? new SawmillRecipes() : null;
        cooking = switch (kind) {
            case ELECTRIC_FURNACE -> RecipeManager.createCheck(RecipeType.SMELTING);
            case ELECTRIC_BLAST_FURNACE -> RecipeManager.createCheck(RecipeType.BLASTING);
            case ELECTRIC_SMOKER -> RecipeManager.createCheck(RecipeType.SMOKING);
            default -> null;
        };
        pumpTank = kind == MachineKind.PUMP ? new PumpTank(this::setChanged) : null;
        geothermalTank = kind == MachineKind.GEOTHERMAL
            ? new FluidTank(GeothermalGeneratorBlock.TANK_CAPACITY, stack -> stack.getFluid() == Fluids.LAVA) {
                @Override
                protected void onContentsChanged() {
                    setChanged();
                }
            } : null;
        inventory = new ItemStackHandler(switch (kind) {
            case WIND_TURBINE -> 1;
            case GEOTHERMAL, ALLOY_SMELTER -> SLOT_COUNT + 1;
            default -> SLOT_COUNT;
        }) {
            @Override
            public boolean isItemValid(int slot, ItemStack stack) {
                if (kind.isStorage()) {
                    return RechargeableItem.supports(stack, kind.storageOutput, slot == INPUT_SLOT);
                }
                if (kind == MachineKind.GEOTHERMAL && slot == GEOTHERMAL_BATTERY_SLOT) {
                    return RechargeableItem.supports(stack, MachineKind.BATBOX.storageOutput);
                }
                if (kind == MachineKind.ALLOY_SMELTER && slot == SECOND_INPUT_SLOT) {
                    return AlloyRecipes.accepts(stack, getStackInSlot(INPUT_SLOT));
                }
                if (kind == MachineKind.ELECTROLYZER && slot == OUTPUT_SLOT) {
                    return Electrolysis.discharge(stack) != null;
                }
                return slot == INPUT_SLOT && accepts(stack);
            }

            @Override
            protected void onContentsChanged(int slot) {
                if (slot == INPUT_SLOT && kind.recipeTicks > 0 && kind != MachineKind.PUMP) {
                    trackInput();
                }
                if (kind == MachineKind.ALLOY_SMELTER && slot == SECOND_INPUT_SLOT) {
                    trackInput();
                }
                setChanged();
            }
        };
        var input = inputOnly(INPUT_SLOT);
        automationInput = switch (kind) {
            case ALLOY_SMELTER -> new CombinedInvWrapper(input, inputOnly(SECOND_INPUT_SLOT));
            case ELECTROLYZER -> new RangedWrapper(inventory, INPUT_SLOT, INPUT_SLOT + 1);
            default -> input;
        };
        var output = kind == MachineKind.WIND_TURBINE ? input : new RangedWrapper(inventory, OUTPUT_SLOT, OUTPUT_SLOT + 1);
        automationOutput = kind == MachineKind.GEOTHERMAL
            ? new CombinedInvWrapper(output, new BatterySlot(inventory, GEOTHERMAL_BATTERY_SLOT, true))
            : output;
        batteryChargeSlot = new BatterySlot(inventory, INPUT_SLOT, true);
        batteryDischargeSlot = new BatterySlot(inventory, OUTPUT_SLOT, false);
    }

    private RangedWrapper inputOnly(int slot) {
        return new RangedWrapper(inventory, slot, slot + 1) {
            @Override
            public ItemStack extractItem(int slot, int amount, boolean simulate) {
                return ItemStack.EMPTY;
            }
        };
    }

    static BlockEntityType<MachineEntity> typeFor(BlockState state) {
        return switch (((MachineBlock) state.getBlock()).kind()) {
            case GENERATOR -> Machines.GENERATOR_ENTITY.get();
            case GEOTHERMAL -> Machines.GEOTHERMAL_ENTITY.get();
            case WIND_TURBINE -> Machines.WIND_TURBINE_ENTITY.get();
            case SOLAR_PANEL -> Machines.SOLAR_PANEL_ENTITY.get();
            case LV_TRANSFORMER -> Machines.LV_TRANSFORMER_ENTITY.get();
            case MV_TRANSFORMER -> Machines.MV_TRANSFORMER_ENTITY.get();
            case HV_TRANSFORMER -> Machines.HV_TRANSFORMER_ENTITY.get();
            case EV_TRANSFORMER -> Machines.EV_TRANSFORMER_ENTITY.get();
            case MACERATOR -> Machines.MACERATOR_ENTITY.get();
            case ELECTRIC_FURNACE -> Machines.ELECTRIC_FURNACE_ENTITY.get();
            case ELECTRIC_BLAST_FURNACE -> Machines.ELECTRIC_BLAST_FURNACE_ENTITY.get();
            case ELECTRIC_SMOKER -> Machines.ELECTRIC_SMOKER_ENTITY.get();
            case SAWMILL -> Machines.SAWMILL_ENTITY.get();
            case BATBOX -> Machines.BATBOX_ENTITY.get();
            case MFE -> Machines.MFE_ENTITY.get();
            case MFSU -> Machines.MFSU_ENTITY.get();
            case ALLOY_SMELTER -> Machines.ALLOY_SMELTER_ENTITY.get();
            case EXTRACTOR -> Machines.EXTRACTOR_ENTITY.get();
            case RARE_EARTH_EXTRACTOR -> Machines.RARE_EARTH_ENTITY.get();
            case ELECTROLYZER -> Machines.ELECTROLYZER_ENTITY.get();
            case COMPRESSOR -> Machines.COMPRESSOR_ENTITY.get();
            case PUMP -> Machines.PUMP_ENTITY.get();
            case RECYCLER -> Machines.RECYCLER_ENTITY.get();
            case MASS_FABRICATOR -> Machines.MASS_FABRICATOR_ENTITY.get();
        };
    }

    void tick() {
        switch (kind) {
            case GENERATOR -> tickGenerator();
            case GEOTHERMAL -> tickGeothermal();
            case WIND_TURBINE -> tickWindTurbine();
            case SOLAR_PANEL -> tickSolarPanel();
            case LV_TRANSFORMER, MV_TRANSFORMER, HV_TRANSFORMER, EV_TRANSFORMER -> tickTransformer();
            case PUMP -> tickPump();
            case MASS_FABRICATOR -> tickMassFabricator();
            case MACERATOR, ELECTRIC_FURNACE, EXTRACTOR, COMPRESSOR, RECYCLER -> tickProcessor();
            case ALLOY_SMELTER, ELECTRIC_BLAST_FURNACE, ELECTRIC_SMOKER, SAWMILL -> setActive(tickProcessor());
            case RARE_EARTH_EXTRACTOR -> setActive(tickRareEarth());
            case ELECTROLYZER -> setActive(tickElectrolyzer());
            case BATBOX, MFE, MFSU -> tickStorage();
        }
    }

    private void tickWindTurbine() {
        if (windProduction < 0 || (level.getGameTime() + worldPosition.hashCode()) % WindProduction.SAMPLE_INTERVAL == 0) {
            windProduction = WindProduction.sample((ServerLevel) level, worldPosition);
        }
        boolean generating = windProduction > 0 && energy < kind.capacity;
        if (generating) {
            int production = windRemainder + windProduction;
            energy = Math.min(kind.capacity, energy + production / WindProduction.UNITS_PER_EU);
            windRemainder = production % WindProduction.UNITS_PER_EU;
            setChanged();
        }
        transferBattery(INPUT_SLOT, true);
        if (energy > 0) {
            sendEnergy(Math.min(energy, WindProduction.OUTPUT_LIMIT), side -> true);
        }
        setActive(generating);
    }

    private void tickGeothermal() {
        refillGeothermalTank();
        boolean generated = energy == 0 && geothermalTank.getFluidAmount() > 0;
        if (generated) {
            geothermalTank.drain(1, FluidAction.EXECUTE);
            energy = GeothermalGeneratorBlock.OUTPUT;
        }
        transferBattery(GEOTHERMAL_BATTERY_SLOT, true);
        if (energy > 0) {
            sendEnergy(energy, side -> true);
        }
        setActive(generated && energy < kind.capacity);
    }

    private void refillGeothermalTank() {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty() || geothermalTank.getFluidAmount() > geothermalTank.getCapacity() - PumpTank.BUCKET) {
            return;
        }
        var preview = FluidUtil.tryEmptyContainer(input, geothermalTank, Integer.MAX_VALUE, null, false);
        if (preview.isSuccess() && canAddOutput(preview.getResult())) {
            var drained = FluidUtil.tryEmptyContainer(input, geothermalTank, Integer.MAX_VALUE, null, true);
            if (drained.isSuccess()) {
                inventory.extractItem(INPUT_SLOT, 1, false);
                addOutput(drained.getResult());
            }
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
        fillPumpContainer();
    }

    private void fillPumpContainer() {
        if (pumpTank.getFluidAmount() < PumpTank.BUCKET) {
            return;
        }
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        var fluid = pumpTank.getFluid().getFluid();
        ItemStack filled;
        if (input.is(Items.BUCKET)) {
            filled = new ItemStack(fluid.getBucket());
        } else if (input.is(IC2.CELL_EMPTY.get())) {
            filled = FluidCellItem.filledStack(fluid);
        } else {
            return;
        }
        if (!filled.isEmpty() && canAddOutput(filled)) {
            pumpTank.drain(PumpTank.BUCKET, FluidAction.EXECUTE);
            inventory.extractItem(INPUT_SLOT, 1, false);
            addOutput(filled);
        }
    }

    private void tickStorage() {
        transferBattery(INPUT_SLOT, true);
        transferBattery(OUTPUT_SLOT, false);
        if (energy >= kind.storageOutput) {
            sendEnergy(kind.storageOutput, outputSide()::equals);
        }
    }

    private void tickSolarPanel() {
        boolean active = SolarPanelBlock.hasSun(level, worldPosition);
        setActive(active);
        if (active) {
            var panel = (SolarPanelBlock) getBlockState().getBlock();
            EnergyNetwork.send(level, worldPosition, panel.output(), panel::emitsTo, kind);
        }
    }

    private void tickTransformer() {
        boolean stepUp = level.hasNeighborSignal(worldPosition);
        setActive(stepUp);
        int packet = transformer().packetSize(stepUp);
        int packetCount = stepUp ? 1 : TransformerBlock.LOW_PACKETS;
        Direction highSide = outputSide();
        for (int index = 0; index < packetCount && energy >= packet; index++) {
            if (sendEnergy(packet, side -> stepUp == (side == highSide)) == 0) {
                break;
            }
        }
    }

    private void setActive(boolean active) {
        BlockState state = getBlockState();
        if (state.getValue(BlockStateProperties.LIT) != active) {
            level.setBlock(worldPosition, state.setValue(BlockStateProperties.LIT, active), Block.UPDATE_CLIENTS);
        }
    }

    private void transferBattery(int slot, boolean charging) {
        ItemStack battery = inventory.getStackInSlot(slot);
        if (!(battery.getItem() instanceof RechargeableItem cell) || !inventory.isItemValid(slot, battery)) {
            return;
        }
        int charge = cell.charge(battery);
        int available = charging
            ? Math.min(energy, cell.capacity() - charge)
            : Math.min(kind.capacity - energy, charge);
        int moved = Math.min(cell.transferRate(), available);
        if (moved > 0) {
            int batteryGain = charging ? moved : -moved;
            ItemStack updated = battery.copy();
            cell.setCharge(updated, charge + batteryGain);
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
            sendEnergy(Math.min(GENERATOR_OUTPUT, energy), side -> true);
        }
    }

    private int sendEnergy(int available, Predicate<Direction> emitsTo) {
        int sent = EnergyNetwork.send(level, worldPosition, available, emitsTo, kind);
        if (sent > 0) {
            energy -= sent;
            setChanged();
        }
        return sent;
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
            case GEOTHERMAL -> GeothermalGeneratorBlock.hasLava(stack);
            case WIND_TURBINE -> RechargeableItem.supports(stack, MachineKind.BATBOX.storageOutput);
            case MACERATOR -> macerationRecipe(new SingleRecipeInput(stack)).isPresent();
            case ELECTRIC_FURNACE, ELECTRIC_BLAST_FURNACE, ELECTRIC_SMOKER ->
                furnaceRecipe(new SingleRecipeInput(stack)).isPresent();
            case SAWMILL -> !sawmill.result(stack, level).isEmpty();
            case ALLOY_SMELTER -> AlloyRecipes.accepts(stack, secondInput());
            case EXTRACTOR -> ExtractionRecipes.find(stack) != null;
            case RARE_EARTH_EXTRACTOR -> RareEarthRecipes.find(stack) != null;
            case ELECTROLYZER -> Electrolysis.charge(stack) != null;
            case COMPRESSOR -> CompressionRecipes.find(stack) != null;
            case PUMP -> stack.is(Items.BUCKET) || stack.is(IC2.CELL_EMPTY.get());
            case RECYCLER -> RecyclingRecipes.find(stack) != null;
            case MASS_FABRICATOR -> MassFabrication.amplifierValue(stack) > 0;
            case BATBOX, MFE, MFSU, SOLAR_PANEL, LV_TRANSFORMER, MV_TRANSFORMER, HV_TRANSFORMER, EV_TRANSFORMER -> false;
        };
    }

    private Optional<? extends AbstractCookingRecipe> furnaceRecipe(SingleRecipeInput input) {
        return level == null || input.isEmpty() ? Optional.empty() : cooking.getRecipeFor(input, level).map(RecipeHolder::value);
    }

    private Optional<RecipeHolder<MacerationRecipe>> macerationRecipe(SingleRecipeInput input) {
        return level == null || input.isEmpty() ? Optional.empty() : macerating.getRecipeFor(input, level);
    }

    private Work workFor(ItemStack stack) {
        return switch (kind) {
            case MACERATOR -> macerationWork(stack);
            case ELECTRIC_FURNACE, ELECTRIC_BLAST_FURNACE, ELECTRIC_SMOKER -> furnaceWork(stack);
            case SAWMILL -> new Work(1, sawmill.result(stack, level));
            case ALLOY_SMELTER -> {
                var batch = AlloyRecipes.find(stack, inventory.getStackInSlot(SECOND_INPUT_SLOT));
                yield batch == null ? null : new Work(batch.firstCount(), batch.secondCount(), batch.output(), 1, 0);
            }
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
            case GENERATOR, GEOTHERMAL, WIND_TURBINE, BATBOX, MFE, MFSU, SOLAR_PANEL, LV_TRANSFORMER, MV_TRANSFORMER, HV_TRANSFORMER, EV_TRANSFORMER,
                PUMP, MASS_FABRICATOR, RARE_EARTH_EXTRACTOR, ELECTROLYZER -> null;
        };
    }

    private Work macerationWork(ItemStack stack) {
        var input = new SingleRecipeInput(stack);
        return macerationRecipe(input).map(holder -> {
            MacerationRecipe recipe = holder.value();
            int ticks = Math.max(1, recipe.getCookingTime());
            return new Work(1, recipe.assemble(input, level.registryAccess()), 1, ticks - kind.recipeTicks);
        }).orElse(null);
    }

    private Work furnaceWork(ItemStack stack) {
        var input = new SingleRecipeInput(stack);
        return furnaceRecipe(input).map(recipe -> {
            int ticks = kind.cookingTicks(recipe.getCookingTime());
            return new Work(1, recipe.assemble(input, level.registryAccess()), 1, ticks - kind.recipeTicks);
        }).orElse(null);
    }

    private void trackInput() {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        ItemStack second = secondInput();
        if (!ItemStack.isSameItemSameComponents(processingInput, input)
                || !ItemStack.isSameItemSameComponents(processingSecondInput, second)) {
            processingInput = input.copyWithCount(1);
            processingSecondInput = second.copyWithCount(1);
            progress = 0;
        }
    }

    private ItemStack secondInput() {
        return kind == MachineKind.ALLOY_SMELTER ? inventory.getStackInSlot(SECOND_INPUT_SLOT) : ItemStack.EMPTY;
    }

    private boolean tickProcessor() {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        PumpTank waterSource = kind == MachineKind.COMPRESSOR && input.isEmpty() ? findWaterSource() : null;
        Work work = waterSource == null ? workFor(input) : new Work(0, new ItemStack(Items.SNOWBALL));
        if (work == null || work.output().isEmpty() || input.getCount() < work.inputCount()) {
            if (progress != 0) {
                progress = 0;
                setChanged();
            }
            return false;
        }
        if (!canAddOutput(work.output()) || energy < kind.energyPerTick) {
            return false;
        }
        energy -= kind.energyPerTick;
        progress++;
        if (progress >= kind.recipeTicks + work.extraTicks()) {
            if (waterSource == null) {
                inventory.extractItem(INPUT_SLOT, work.inputCount(), false);
                if (work.secondInputCount() > 0) {
                    inventory.extractItem(SECOND_INPUT_SLOT, work.secondInputCount(), false);
                }
            } else {
                waterSource.drain(new FluidStack(Fluids.WATER, PumpTank.BUCKET), FluidAction.EXECUTE);
            }
            if (work.chanceDenominator() == 1 || level.random.nextInt(work.chanceDenominator()) == 0) {
                addOutput(work.output());
            }
            progress = 0;
        }
        setChanged();
        return true;
    }

    private boolean tickRareEarth() {
        var entry = RareEarthRecipes.find(inventory.getStackInSlot(INPUT_SLOT));
        if (entry == null || energy < kind.energyPerTick) {
            int nextProgress = entry == null ? 0 : Math.max(0, progress - 1);
            if (progress != nextProgress) {
                progress = nextProgress;
                setChanged();
            }
            return false;
        }

        int existing = entry.material() == accumulatedMaterial ? materialUnits : 0;
        int total = existing + entry.units();
        boolean producesDust = total >= RareEarthRecipes.UNITS_PER_OUTPUT;
        ItemStack output = producesDust ? entry.material().output() : ItemStack.EMPTY;
        if (!canAddOutput(output)) {
            return false;
        }

        accumulatedMaterial = entry.material();
        materialUnits = existing;
        energy -= kind.energyPerTick;
        progress++;
        if (progress >= kind.recipeTicks) {
            inventory.extractItem(INPUT_SLOT, 1, false);
            materialUnits = producesDust ? total - RareEarthRecipes.UNITS_PER_OUTPUT : total;
            addOutput(output);
            progress = 0;
        }
        setChanged();
        return true;
    }

    private boolean tickElectrolyzer() {
        MachineEntity storage = adjacentStorage();
        if (storage == null) {
            return false;
        }
        int rate = storage.kind.storageOutput / 16;
        if (storage.energy * 10L < storage.kind.capacity * 3L) {
            return dischargeElectrolysis(storage, rate);
        }
        return !electrolysisDischarging && storage.energy * 10L >= storage.kind.capacity * 7L
            && chargeElectrolysis(storage, rate);
    }

    private boolean chargeElectrolysis(MachineEntity storage, int rate) {
        var recipe = Electrolysis.charge(inventory.getStackInSlot(INPUT_SLOT));
        if (recipe == null) {
            return false;
        }
        ItemStack result = new ItemStack(recipe.output());
        if (!canAddOutput(result)) {
            return false;
        }

        int drawn = Math.min(recipe.energy() - energy, rate);
        storage.energy -= drawn;
        storage.setChanged();
        energy += drawn;
        if (energy == recipe.energy()) {
            inventory.extractItem(INPUT_SLOT, 1, false);
            addOutput(result);
            energy = 0;
        }
        setChanged();
        return true;
    }

    private boolean dischargeElectrolysis(MachineEntity storage, int rate) {
        if (energy > 0) {
            energy -= storage.receiveEnergy(Math.min(energy, rate));
            electrolysisDischarging = energy > 0;
            setChanged();
            return true;
        }

        var recipe = Electrolysis.discharge(inventory.getStackInSlot(OUTPUT_SLOT));
        if (recipe == null) {
            return false;
        }
        ItemStack result = new ItemStack(recipe.input());
        if (!canAddToSlot(INPUT_SLOT, result)) {
            return false;
        }
        int tier = switch (storage.kind) {
            case BATBOX -> 1;
            case MFE -> 2;
            case MFSU -> 3;
            default -> throw new IllegalStateException("Expected an EU storage block");
        };
        inventory.extractItem(OUTPUT_SLOT, 1, false);
        addToSlot(INPUT_SLOT, result);
        energy = recipe.dischargeEnergy(tier);
        electrolysisDischarging = true;
        setChanged();
        return true;
    }

    private MachineEntity adjacentStorage() {
        for (Direction side : Direction.values()) {
            BlockPos pos = worldPosition.relative(side);
            if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof MachineEntity machine && machine.kind.isStorage()) {
                return machine;
            }
        }
        return null;
    }

    private record Work(int inputCount, int secondInputCount, ItemStack output, int chanceDenominator, int extraTicks) {
        Work(int inputCount, ItemStack output, int chanceDenominator, int extraTicks) {
            this(inputCount, 0, output, chanceDenominator, extraTicks);
        }

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
        return canAddToSlot(OUTPUT_SLOT, addition);
    }

    private boolean canAddToSlot(int slot, ItemStack addition) {
        ItemStack current = inventory.getStackInSlot(slot);
        return addition.isEmpty() || (current.isEmpty() && addition.getCount() <= addition.getMaxStackSize())
            || (ItemStack.isSameItemSameComponents(current, addition)
                && current.getCount() + addition.getCount() <= current.getMaxStackSize());
    }

    private void addOutput(ItemStack addition) {
        addToSlot(OUTPUT_SLOT, addition);
    }

    private void addToSlot(int slot, ItemStack addition) {
        if (!addition.isEmpty()) {
            int count = inventory.getStackInSlot(slot).getCount() + addition.getCount();
            inventory.setStackInSlot(slot, addition.copyWithCount(count));
        }
    }

    boolean canReceiveEnergy(Direction incomingSide, MachineKind sourceKind, int packetSize) {
        if (energy >= kind.capacity) {
            return false;
        }
        return switch (kind) {
            case GENERATOR, GEOTHERMAL, WIND_TURBINE, SOLAR_PANEL, ELECTROLYZER -> false;
            case ELECTRIC_BLAST_FURNACE, ELECTRIC_SMOKER, SAWMILL -> packetSize <= MachineKind.BATBOX.storageOutput;
            case MACERATOR, ELECTRIC_FURNACE, ALLOY_SMELTER, EXTRACTOR, RARE_EARTH_EXTRACTOR,
                COMPRESSOR, PUMP, RECYCLER -> true;
            case MASS_FABRICATOR -> !level.hasNeighborSignal(worldPosition);
            case LV_TRANSFORMER, MV_TRANSFORMER, HV_TRANSFORMER, EV_TRANSFORMER -> canReceiveTransformerEnergy(incomingSide, packetSize);
            // Only charge up the storage tiers, so a cable loop cannot feed energy back.
            case BATBOX, MFE, MFSU -> sourceKind.storageOutput < kind.storageOutput && incomingSide != outputSide();
        };
    }

    private boolean canReceiveTransformerEnergy(Direction side, int packetSize) {
        boolean stepUp = level.hasNeighborSignal(worldPosition);
        int maxPacket = transformer().packetSize(!stepUp);
        return packetSize <= maxPacket && stepUp == (side != outputSide());
    }

    private TransformerBlock transformer() {
        return (TransformerBlock) getBlockState().getBlock();
    }

    private Direction outputSide() {
        return getBlockState().getValue(BlockStateProperties.FACING);
    }

    Component storageStatus() {
        return Component.translatable("message.ic2.batbox_status", energy, kind.capacity, kind.storageOutput,
            Component.translatable("direction.ic2." + outputSide().getName()));
    }

    Component transformerStatus() {
        String mode = level.hasNeighborSignal(worldPosition) ? "up" : "down";
        return Component.translatable("message.ic2.transformer_" + mode, energy, kind.capacity, transformer().packetSize(true),
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

    static int fuelTicks(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) / 4;
    }

    IItemHandler automation(Direction side) {
        if (kind == MachineKind.WIND_TURBINE) {
            return side == Direction.UP ? null : batteryChargeSlot;
        }
        if (kind == MachineKind.ELECTROLYZER) {
            return switch (side) {
                case UP -> automationInput;
                case DOWN -> automationOutput;
                case null, default -> null;
            };
        }
        if (kind == MachineKind.PUMP) {
            if (side == Direction.UP) {
                return automationInput;
            }
            return side != null && side.getAxis().isHorizontal() ? automationOutput : null;
        }
        if (kind.isStorage()) {
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

    IFluidHandler geothermalFluid(Direction side) {
        return geothermalTank;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory playerInventory, Player player) {
        return new MachineMenu(id, playerInventory, inventory, ContainerLevelAccess.create(level, worldPosition),
            getBlockState().getBlock(), () -> energy,
            this::menuWork, this::menuFluid,
            this::setChanged, switch (kind) {
                case BATBOX -> Machines.BATBOX_MENU.get();
                case MFE -> Machines.MFE_MENU.get();
                case MFSU -> Machines.MFSU_MENU.get();
                case ALLOY_SMELTER -> Machines.ALLOY_SMELTER_MENU.get();
                case RARE_EARTH_EXTRACTOR -> Machines.RARE_EARTH_MENU.get();
                case ELECTROLYZER -> Machines.ELECTROLYZER_MENU.get();
                case MASS_FABRICATOR -> Machines.FABRICATOR_MENU.get();
                case GEOTHERMAL -> Machines.GEOTHERMAL_MENU.get();
                case WIND_TURBINE -> Machines.WIND_TURBINE_MENU.get();
                default -> Machines.MENU.get();
            });
    }

    private int menuFluid() {
        if (kind == MachineKind.RARE_EARTH_EXTRACTOR) {
            return materialUnits * 100 / RareEarthRecipes.UNITS_PER_OUTPUT;
        }
        if (geothermalTank != null) {
            return geothermalTank.getFluidAmount();
        }
        return pumpTank == null ? -1 : pumpTank.getFluidAmount();
    }

    private int menuWork() {
        return switch (kind) {
            case GENERATOR -> burnTicks;
            case WIND_TURBINE -> Math.max(0, windProduction);
            case BATBOX, MFE, MFSU -> outputSide().ordinal();
            case SOLAR_PANEL, GEOTHERMAL, LV_TRANSFORMER, MV_TRANSFORMER, HV_TRANSFORMER, EV_TRANSFORMER, ELECTROLYZER -> 0;
            case MASS_FABRICATOR -> amplifier;
            case MACERATOR, ELECTRIC_FURNACE, ELECTRIC_BLAST_FURNACE, ELECTRIC_SMOKER, SAWMILL,
                ALLOY_SMELTER, EXTRACTOR, RARE_EARTH_EXTRACTOR,
                COMPRESSOR, PUMP, RECYCLER -> progress;
        };
    }

    void dropContents(Level world) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
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
        } else if (kind == MachineKind.GEOTHERMAL) {
            tag.putInt("lava", geothermalTank.getFluidAmount());
        } else if (kind == MachineKind.WIND_TURBINE) {
            tag.putInt("wind_remainder", windRemainder);
        } else if (kind == MachineKind.ELECTROLYZER) {
            tag.putBoolean("electrolysis_discharging", electrolysisDischarging);
        } else if (kind == MachineKind.MASS_FABRICATOR) {
            tag.putInt("amplifier", amplifier);
            tag.putInt("amplifier_refill", amplifierRefillThreshold);
        } else if (kind.recipeTicks > 0) {
            tag.putInt("progress", progress);
            if (kind == MachineKind.RARE_EARTH_EXTRACTOR) {
                tag.putString("material", accumulatedMaterial == null ? "" : accumulatedMaterial.id);
                tag.putInt("material_units", materialUnits);
            }
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
        } else if (kind == MachineKind.GEOTHERMAL) {
            geothermalTank.setFluid(new FluidStack(Fluids.LAVA,
                Mth.clamp(tag.getInt("lava"), 0, geothermalTank.getCapacity())));
        } else if (kind == MachineKind.WIND_TURBINE) {
            windRemainder = Mth.clamp(tag.getInt("wind_remainder"), 0, WindProduction.UNITS_PER_EU - 1);
        } else if (kind == MachineKind.ELECTROLYZER) {
            electrolysisDischarging = energy > 0 && tag.getBoolean("electrolysis_discharging");
        } else if (kind == MachineKind.MASS_FABRICATOR) {
            amplifier = Mth.clamp(tag.getInt("amplifier"), 0, 2_000_000);
            amplifierRefillThreshold = Mth.clamp(tag.getInt("amplifier_refill"), 0, 1_000_000);
        } else if (kind.recipeTicks > 0) {
            processingInput = inventory.getStackInSlot(INPUT_SLOT).copyWithCount(1);
            processingSecondInput = secondInput().copyWithCount(1);
            int maxProgress = switch (kind) {
                case MACERATOR, ELECTRIC_BLAST_FURNACE, ELECTRIC_SMOKER -> Integer.MAX_VALUE - 1;
                case PUMP -> kind.recipeTicks;
                default -> kind.recipeTicks - 1;
            };
            if (kind == MachineKind.RECYCLER && processingInput.is(IC2.SCRAP.get())) {
                maxProgress += RecyclingRecipes.BOX_EXTRA_TICKS;
            }
            progress = Mth.clamp(tag.getInt("progress"), 0, maxProgress);
            if (kind == MachineKind.RARE_EARTH_EXTRACTOR) {
                accumulatedMaterial = RareEarthRecipes.Material.byId(tag.getString("material"));
                materialUnits = accumulatedMaterial == null ? 0
                    : Mth.clamp(tag.getInt("material_units"), 0, RareEarthRecipes.UNITS_PER_OUTPUT - 1);
            }
        }
    }
}
