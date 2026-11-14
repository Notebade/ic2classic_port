package ic2.core.machine;

import ic2.api.machine.MachineSnapshot;

import ic2.core.item.RechargeableItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.CombinedInvWrapper;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

final class AdvancedMachineEntity extends BlockEntity implements MenuProvider, EnergySink, MachineSnapshot.Provider, MachineAudio {
    @Override public MachineSnapshot machineInfo() {
        return MachineTelemetry.snapshot(this, energy, modifiers().capacity(), MachineSnapshot.EnergyUnit.EU,
            java.util.Map.of("heat", (long) heat, "progress_units", (long) progress, "progress_target", (long) duration,
                "processing", processing ? 1L : 0L, "input_packet_eu", (long) modifiers().packet(),
                "sound_multiplier_permille", (long) (modifiers().sound() * 1000)));
    }
    final AdvancedMachines.Variant variant = ((AdvancedMachineBlock) getBlockState().getBlock()).variant;
    private int energy;
    private int heat;
    private int progress;
    private int duration = 120000;
    private boolean split = true;
    private boolean processing;
    private float experience;
    private RareEarthProgress rareEarth = new RareEarthProgress("", 0);
    private int grinderMode = 1;
    private int queueTick;
    private boolean pressureWasFormed;
    private Direction pressureFacing;
    private final ItemStack[] trackedInputs = emptyInputs(variant.layout.inputs());
    final ItemStackHandler inventory = new ItemStackHandler(variant.layout.slots() + 2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if (slot >= variant.layout.slots()) return acceptsUpgrade(stack);
            if (slot == 0) return RechargeableItem.supports(stack, modifiers().packet(), false);
            if (!variant.layout.isInput(slot)) return false;
            return variant == AdvancedMachines.Variant.PRESSURE
                ? AlloyRecipes.accepts(stack, getStackInSlot(slot == 1 ? 2 : 1)) : recipe(stack) != null;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (variant.layout.isInput(slot)) {
                ItemStack next = getStackInSlot(slot);
                if ((variant != AdvancedMachines.Variant.SLOW || slot == 1)
                        && !ItemStack.isSameItemSameComponents(trackedInputs[slot - 1], next)) progress = 0;
                trackedInputs[slot - 1] = next.copyWithCount(1);
            }
            if (slot >= variant.layout.slots()) energy = Math.min(energy, modifiers().capacity());
            setChanged();
            if (slot >= variant.layout.slots()) syncAudio();
        }
    };
    private final RangedWrapper input = new RangedWrapper(inventory, 1, variant.layout.inputs() + 1) {
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
    };
    private final RangedWrapper output = new RangedWrapper(inventory, variant.layout.inputs() + 1, variant.layout.slots());
    private final BatterySlot battery = new BatterySlot(inventory, 0, false);
    private final IItemHandler bottom = new CombinedInvWrapper(output, battery);
    private final IItemHandler sides = new CombinedInvWrapper(battery, input, output);
    final ContainerData data = new ContainerData() {
        @Override public int getCount() { return 15; }
        @Override public void set(int index, int value) {}
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> energy & 65535;
                case 1 -> heat;
                case 2 -> (int) (1000L * progress / duration);
                case 3 -> split ? 1 : 0;
                case 4 -> processing ? 1 : 0;
                case 5 -> rareEarth.units() * 1000 / RareEarthProgress.UNITS_PER_OUTPUT;
                case 6 -> grinderMode;
                case 7 -> energy >>> 16;
                case 8 -> modifiers().capacity() & 65535;
                case 9 -> modifiers().capacity() >>> 16;
                case 10 -> modifiers().packet() & 65535;
                case 11 -> modifiers().packet() >>> 16;
                case 12 -> (int) Math.min(Integer.MAX_VALUE, modifiers().sound() * 1000);
                case 13 -> variant.ordinal();
                case 14 -> heat >>> 16;
                default -> throw new IndexOutOfBoundsException(index);
            };
        }
    };

    AdvancedMachineEntity(BlockPos pos, BlockState state) {
        super(AdvancedMachines.ENTITY.get(), pos, state);
    }
    private boolean acceptsUpgrade(ItemStack stack) {
        if (!(stack.getItem() instanceof ic2.api.machine.MachineUpgrade upgrade)) return false;
        var category = upgrade.category(stack);
        return category != ic2.api.machine.MachineUpgrade.Category.RECIPE
            && (variant != AdvancedMachines.Variant.SLOW || category != ic2.api.machine.MachineUpgrade.Category.MACHINE);
    }
    private MachineModifiers modifiers() {
        return MachineModifiers.read(inventory, variant.layout.slots(), variant.capacity(), variant == AdvancedMachines.Variant.PRESSURE ? 3 : 2);
    }
    private boolean standbyHeat() { return modifiers().redstoneAllowed(true, level.hasNeighborSignal(worldPosition)); }
    private ic2.api.machine.UpgradeContext upgradeContext() {
        return new ic2.api.machine.UpgradeContext((ServerLevel) level, worldPosition,
            new RangedWrapper(inventory, 1, variant.layout.inputs() + 1), output, java.util.Optional.empty());
    }
    private void tickUpgrades() {
        for (int slot = variant.layout.slots(); slot < inventory.getSlots(); slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof ic2.api.machine.MachineUpgrade upgrade && upgrade.tickUpgrade(stack, upgradeContext())) { setChanged(); syncAudio(); }
        }
    }
    private void syncAudio() {
        if (level != null && !level.isClientSide) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
    private void setProcessing(boolean next) {
        if (processing == next) return;
        if (processing && !next && progress > 0 && !inventory.getStackInSlot(1).isEmpty() && variant.sound() != null)
            level.playSound(null, worldPosition, NuclearInfrastructure.SOUNDS.get("machine_interruption").get(), net.minecraft.sounds.SoundSource.BLOCKS, .5F * audioMultiplier(), 1);
        processing = next; syncAudio();
    }
    @Override public boolean audioWorking() { return processing; }
    @Override public float audioMultiplier() { return (float) modifiers().sound(); }
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider lookup) {
        var tag = saveWithoutMetadata(lookup); tag.putBoolean("audio_processing", processing); return tag;
    }
    @Override public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    void tick() {
        if (variant == AdvancedMachines.Variant.PRESSURE && !updatePressureStructure()) return;
        tickUpgrades();
        if (!isOperational()) return;
        dischargeBattery();
        if (variant == AdvancedMachines.Variant.SLOW) {
            tickSlowGrinder();
            return;
        }
        if (variant == AdvancedMachines.Variant.RARE_EARTH) {
            tickRareEarth();
            return;
        }
        ProcessingRecipe first = availableRecipe(1);
        ProcessingRecipe second = variant.layout.supportsSplit() ? availableRecipe(2) : null;
        boolean work = first != null || second != null;
        if (work) duration = Math.max(first == null ? 0 : first.duration(), second == null ? 0 : second.duration());
        var step = InductionCycle.step(heat, progress, energy, work, standbyHeat(), MachineUpgradeRules.scale(variant.cost, modifiers().demand()), duration);
        boolean changed = step.spent() > 0 || heat != step.heat() || progress != step.progress();
        energy -= step.spent();
        heat = step.heat();
        progress = step.progress();
        setProcessing(step.processing());
        if (step.finished()) {
            for (int pass = 0; pass < variant.layout.processingPasses(); pass++) {
                process(variant.layout.inputForPass(pass, split, inventory.getStackInSlot(1).isEmpty(),
                    variant.layout.supportsSplit() && inventory.getStackInSlot(2).isEmpty()));
            }
        }
        updateLit(step.heating());
        if (changed) setChanged();
    }

    boolean isOperational() { return level != null && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this
        && (variant != AdvancedMachines.Variant.PRESSURE || pressureLive()); }

    private java.util.List<BlockPos> pressurePositions() {
        Direction front = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        return PressureStructure.offsets(front.getStepX(), front.getStepZ()).stream()
            .map(offset -> worldPosition.offset(offset.x(), offset.y(), offset.z())).toList();
    }

    boolean pressureContains(BlockPos part) {
        return variant == AdvancedMachines.Variant.PRESSURE && pressurePositions().contains(part);
    }

    boolean pressureShapeValid() {
        return variant == AdvancedMachines.Variant.PRESSURE && level != null && pressurePositions().stream()
            .allMatch(pos -> level.isLoaded(pos) && level.getBlockState(pos).is(AdvancedMachines.PRESSURE_PART.get()));
    }

    boolean pressureLive() {
        return pressureWasFormed && pressureFacing == getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING)
            && pressureShapeValid() && pressurePositions().stream().allMatch(pos ->
                level.getBlockEntity(pos) instanceof PressurePartEntity part && part.ownedBy(worldPosition));
    }

    private boolean formPressureStructure() {
        if (level == null || level.isClientSide || !pressureShapeValid()) return false;
        var parts = new java.util.ArrayList<PressurePartEntity>(7);
        for (BlockPos pos : pressurePositions()) {
            if (!(level.getBlockEntity(pos) instanceof PressurePartEntity part) || !part.canClaim(worldPosition)) return false;
            parts.add(part);
        }
        for (var part : parts) part.claim(worldPosition);
        return true;
    }

    private boolean updatePressureStructure() {
        boolean formed = formPressureStructure();
        if (formed != pressureWasFormed) {
            pressureWasFormed = formed;
            level.invalidateCapabilities(worldPosition);
            for (BlockPos pos : pressurePositions()) {
                if (level.isLoaded(pos)) level.invalidateCapabilities(pos);
            }
        }
        Direction facing = getBlockState().getValue(BlockStateProperties.HORIZONTAL_FACING);
        if (!formed || pressureFacing != null && pressureFacing != facing) {
            if (heat != 0 || progress != 0) {
                heat = 0;
                progress = 0;
                setChanged();
            }
        }
        pressureFacing = facing;
        if (!formed) {
            setProcessing(false);
            updateLit(false);
        }
        return formed;
    }

    private static ItemStack[] emptyInputs(int count) {
        ItemStack[] values = new ItemStack[count];
        java.util.Arrays.fill(values, ItemStack.EMPTY);
        return values;
    }

    private void tickSlowGrinder() {
        if (++queueTick % 10 == 0) rotateQueue();
        ProcessingRecipe recipe = availableRecipe(1);
        if (recipe != null) duration = recipe.duration();
        var step = RecyclerControl.slowStep(progress, energy, grinderMode, duration,
            recipe != null && modifiers().redstoneAllowed(false, level.hasNeighborSignal(worldPosition)));
        boolean changed = step.spent() > 0 || progress != step.progress();
        progress = step.progress();
        energy -= step.spent();
        setProcessing(step.processing());
        if (step.finished()) process(1);
        updateLit(processing);
        if (changed) setChanged();
    }

    private void rotateQueue() {
        for (int slot = 2; slot <= 9; slot++) {
            ItemStack from = inventory.getStackInSlot(slot);
            if (from.isEmpty()) continue;
            int target = RecyclerControl.nextQueueSlot(slot);
            ItemStack to = inventory.getStackInSlot(target);
            int moved = RecyclerControl.queueMove(from.getCount(), to.getCount(), from.getMaxStackSize(),
                to.isEmpty() || ItemStack.isSameItemSameComponents(from, to));
            if (moved > 0) {
                inventory.setStackInSlot(target, from.copyWithCount(to.getCount() + moved));
                inventory.setStackInSlot(slot, from.copyWithCount(from.getCount() - moved));
            }
        }
    }

    void cycleGrinderMode() {
        grinderMode = grinderMode % 5 + 1;
        setChanged();
    }

    private void tickRareEarth() {
        ItemStack input = inventory.getStackInSlot(1);
        var entry = RareEarthRecipes.find(input);
        RareEarthProgress selected = entry == null ? rareEarth : rareEarth.select(entry.material().id);
        var batch = entry == null ? null : selected.add(entry.units());
        ItemStack output = entry == null ? ItemStack.EMPTY : entry.material().output();
        boolean work = batch != null && (batch.batches() == 0 || room(1, output) >= output.getCount());
        duration = 99 * 300;
        var step = InductionCycle.step(heat, progress, energy, work,
            !input.isEmpty() || standbyHeat(), MachineUpgradeRules.scale(variant.cost, modifiers().demand()), duration);
        int nextProgress = work ? step.progress() : 0;
        if (!step.processing() && work) nextProgress = Math.max(0, progress - 300);
        boolean changed = step.spent() > 0 || heat != step.heat() || progress != nextProgress;
        energy -= step.spent();
        heat = step.heat();
        progress = nextProgress;
        setProcessing(step.processing());
        if (processing) rareEarth = selected;
        if (step.finished()) {
            rareEarth = batch.next();
            inventory.extractItem(1, 1, false);
            if (batch.batches() > 0) {
                ItemStack current = inventory.getStackInSlot(2);
                inventory.setStackInSlot(2, output.copyWithCount(current.getCount() + output.getCount()));
            }
            notifyRecipeFinished(batch.batches() > 0 ? output : ItemStack.EMPTY, 0);
        }
        updateLit(step.heating());
        if (changed) setChanged();
    }

    private void updateLit(boolean lit) {
        BlockState state = getBlockState();
        if (state.getValue(BlockStateProperties.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(BlockStateProperties.LIT, lit), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    private ProcessingRecipe recipe(ItemStack stack) {
        if (level == null) return null;
        if (stack.isEmpty()) {
            PumpTank pump = variant == AdvancedMachines.Variant.SINGULARITY
                ? MachineEntity.findWaterSource(level, worldPosition) : null;
            return pump == null ? null : new ProcessingRecipe(new ItemStack(net.minecraft.world.item.Items.SNOWBALL), 120000, 0, 0, pump);
        }
        return switch (variant) {
            case PRESSURE -> {
                var batch = AlloyRecipes.find(stack, inventory.getStackInSlot(2));
                yield batch == null ? null : new ProcessingRecipe(batch.output(), 120000, 0, batch.firstCount(), null, 1, batch.secondCount());
            }
            case COMPACTING, SLOW -> {
                var recipe = RecyclingRecipes.find(stack);
                int time = variant == AdvancedMachines.Variant.SLOW ? 100 + (recipe == null ? 0 : recipe.extraTicks())
                    : RecyclerControl.compactDuration(recipe != null && recipe.inputCount() == 9);
                yield recipe == null ? null : new ProcessingRecipe(new ItemStack(recipe.output(), recipe.count()), time, 0,
                    recipe.inputCount(), null, recipe.chanceDenominator());
            }
            case RARE_EARTH -> {
                var entry = RareEarthRecipes.find(stack);
                yield entry == null ? null : new ProcessingRecipe(entry.material().output(), 99 * 300, 0);
            }
            case ROTARY -> level.getRecipeManager().getRecipeFor(Machines.MACERATING.get(), new SingleRecipeInput(stack), level)
                .map(holder -> new ProcessingRecipe(holder.value().assemble(new SingleRecipeInput(stack), level.registryAccess()),
                    recipeDuration(holder.value().getCookingTime(), 300), holder.value().getExperience())).orElse(null);
            case CENTRIFUGAL -> {
                ItemStack result = ExtractionRecipes.result(stack);
                yield result.isEmpty() ? null : new ProcessingRecipe(result, 120000, 0);
            }
            case SINGULARITY -> {
                var recipe = CompressionRecipes.find(stack);
                yield recipe == null ? null : new ProcessingRecipe(new ItemStack(recipe.output()), 120000, 0, recipe.inputCount(), null);
            }
            default -> cookingRecipe(stack);
        };
    }

    private static int recipeDuration(int ticks, int multiplier) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, ticks) * multiplier);
    }

    private ProcessingRecipe cookingRecipe(ItemStack stack) {
        var input = new SingleRecipeInput(stack);
        return level.getRecipeManager().getRecipeFor(variant.recipes, input, level).map(holder -> {
            var recipe = holder.value();
            int time = recipeDuration(recipe.getCookingTime(), 600);
            return new ProcessingRecipe(recipe.assemble(input, level.registryAccess()), time, recipe.getExperience());
        }).orElse(null);
    }

    private ProcessingRecipe availableRecipe(int slot) {
        ProcessingRecipe recipe = recipe(inventory.getStackInSlot(slot));
        return recipe != null && inventory.getStackInSlot(slot).getCount() >= recipe.inputCount() && !recipe.output().isEmpty()
            && room(slot, recipe.output()) >= recipe.output().getCount() ? recipe : null;
    }

    private int room(int inputSlot, ItemStack result) {
        if (result.isEmpty()) return 0;
        int room = 0;
        for (int slot = variant.layout.firstOutput(inputSlot, split); slot <= variant.layout.lastOutput(inputSlot, split); slot++) {
            ItemStack current = inventory.getStackInSlot(slot);
            if (current.isEmpty() || ItemStack.isSameItemSameComponents(current, result)) {
                room += Math.min(inventory.getSlotLimit(slot), result.getMaxStackSize()) - current.getCount();
            }
        }
        return room;
    }

    private void process(int inputSlot) {
        ProcessingRecipe recipe = availableRecipe(inputSlot);
        if (recipe == null) return;
        boolean success = recipe.chanceDenominator() == 1 || (variant == AdvancedMachines.Variant.SLOW
            ? level.random.nextInt(100) < RecyclerControl.chancePercent(grinderMode, recipe.chanceDenominator())
            : level.random.nextInt(recipe.chanceDenominator()) == 0);
        ItemStack result = success ? recipe.output().copy() : ItemStack.EMPTY;
        for (int slot = variant.layout.firstOutput(inputSlot, split); slot <= variant.layout.lastOutput(inputSlot, split) && !result.isEmpty(); slot++) {
            ItemStack current = inventory.getStackInSlot(slot);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, result)) continue;
            int moved = Math.min(result.getCount(), Math.min(inventory.getSlotLimit(slot), result.getMaxStackSize()) - current.getCount());
            if (moved > 0) {
                inventory.setStackInSlot(slot, result.copyWithCount(current.getCount() + moved));
                result.shrink(moved);
            }
        }
        if (recipe.pump() == null) inventory.extractItem(inputSlot, recipe.inputCount(), false);
        else recipe.pump().drain(PumpTank.BUCKET, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        if (recipe.secondInputCount() > 0) inventory.extractItem(2, recipe.secondInputCount(), false);
        experience += recipe.experience();
        notifyRecipeFinished(success ? recipe.output() : ItemStack.EMPTY, recipe.experience());
    }
    private void notifyRecipeFinished(ItemStack result, float xp) {
        for (int slot = variant.layout.slots(); slot < inventory.getSlots(); slot++) {
            var stack = inventory.getStackInSlot(slot);
            if (stack.getItem() instanceof ic2.api.machine.MachineUpgrade upgrade) upgrade.recipeFinished(stack, upgradeContext(), result.copy(), xp);
        }
    }

    private void dischargeBattery() {
        ItemStack stack = inventory.getStackInSlot(0);
        if (!RechargeableItem.supports(stack, modifiers().packet(), false)) return;
        ic2.api.item.ElectricItem cell = (ic2.api.item.ElectricItem) stack.getItem();
        int stored = cell.charge(stack);
        int moved = Math.min(Math.min(stored, cell.transferRate(stack)), modifiers().capacity() - energy);
        if (moved > 0) {
            ItemStack updated = stack.copy();
            cell.setCharge(updated, stored - moved);
            energy += moved;
            inventory.setStackInSlot(0, updated);
        }
    }

    void toggleSplit() {
        if (!variant.layout.supportsSplit()) return;
        split = !split;
        progress = 0;
        setChanged();
    }

    void awardExperience(Vec3 position) {
        if (!(level instanceof ServerLevel server)) return;
        int amount = Mth.floor(experience);
        if (server.random.nextFloat() < experience - amount) amount++;
        ExperienceOrb.award(server, position, amount);
        experience = 0;
        setChanged();
    }

    void dropContents() {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), inventory.getStackInSlot(slot));
        }
        awardExperience(Vec3.atCenterOf(worldPosition));
    }

    IItemHandler automation(Direction side) {
        if (!isOperational()) return null;
        return GuardedCapabilities.items(() -> !isOperational() ? null
            : side == Direction.UP ? input : side == Direction.DOWN ? bottom : sides);
    }

    @Override public boolean canReceiveEnergy(Direction side, int sourceStorageOutput, int packet) {
        return isOperational() && packet > 0 && packet <= modifiers().packet() && energy < modifiers().capacity();
    }

    @Override public int receiveEnergy(int available) {
        if (!isOperational()) return 0;
        int received = Math.clamp(available, 0, modifiers().capacity() - energy);
        energy += received;
        if (received > 0) setChanged();
        return received;
    }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override public AbstractContainerMenu createMenu(int id, Inventory player, Player owner) {
        return new AdvancedMachineMenu(id, player, inventory, data, ContainerLevelAccess.create(level, worldPosition), this, variant);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy);
        tag.putInt("heat", heat);
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        tag.putBoolean("split", split);
        tag.putFloat("experience", experience);
        if (variant == AdvancedMachines.Variant.SLOW) tag.putInt("grinder_mode", grinderMode);
        if (variant == AdvancedMachines.Variant.RARE_EARTH) {
            tag.putString("rare_material", rareEarth.material());
            tag.putInt("rare_units", rareEarth.units());
        }
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (level != null && level.isClientSide) processing = tag.getBoolean("audio_processing");
        var stored = tag.getCompound("inventory").copy(); stored.putInt("Size", variant.layout.slots() + 2);
        inventory.deserializeNBT(registries, stored);
        for (int i = 0; i < variant.layout.inputs(); i++) trackedInputs[i] = inventory.getStackInSlot(i + 1).copyWithCount(1);
        energy = Mth.clamp(tag.getInt("energy"), 0, modifiers().capacity());
        heat = Mth.clamp(tag.getInt("heat"), 0, InductionCycle.MAX_HEAT);
        duration = Math.max(1, tag.getInt("duration"));
        progress = Mth.clamp(tag.getInt("progress"), 0, duration - 1);
        split = !tag.contains("split") || tag.getBoolean("split");
        experience = Math.max(0, tag.getFloat("experience"));
        grinderMode = Mth.clamp(tag.getInt("grinder_mode"), 1, 5);
        String material = tag.getString("rare_material");
        if (RareEarthRecipes.Material.byId(material) != null) {
            rareEarth = new RareEarthProgress(material, Mth.clamp(tag.getInt("rare_units"), 0, RareEarthProgress.UNITS_PER_OUTPUT - 1));
        }
    }

    private record ProcessingRecipe(ItemStack output, int duration, float experience, int inputCount, PumpTank pump, int chanceDenominator, int secondInputCount) {
        ProcessingRecipe(ItemStack output, int duration, float experience, int inputCount, PumpTank pump, int chanceDenominator) {
            this(output, duration, experience, inputCount, pump, chanceDenominator, 0);
        }
        ProcessingRecipe(ItemStack output, int duration, float experience, int inputCount, PumpTank pump) {
            this(output, duration, experience, inputCount, pump, 1);
        }
        ProcessingRecipe(ItemStack output, int duration, float experience) { this(output, duration, experience, 1, null); }
    }
}
