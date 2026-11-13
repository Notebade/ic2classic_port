package ic2.core.machine;

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

final class AdvancedMachineEntity extends BlockEntity implements MenuProvider, EnergySink {
    static final int PACKET = 128;
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
    private final ItemStack[] trackedInputs = emptyInputs(variant.layout.inputs());
    final ItemStackHandler inventory = new ItemStackHandler(variant.layout.slots()) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 ? RechargeableItem.supports(stack, PACKET, false)
                : variant.layout.isInput(slot) && recipe(stack) != null;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (variant.layout.isInput(slot)) {
                ItemStack next = getStackInSlot(slot);
                if ((variant != AdvancedMachines.Variant.SLOW || slot == 1)
                        && !ItemStack.isSameItemSameComponents(trackedInputs[slot - 1], next)) progress = 0;
                trackedInputs[slot - 1] = next.copyWithCount(1);
            }
            setChanged();
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
        @Override public int getCount() { return 7; }
        @Override public void set(int index, int value) {}
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> energy;
                case 1 -> heat;
                case 2 -> (int) (1000L * progress / duration);
                case 3 -> split ? 1 : 0;
                case 4 -> processing ? 1 : 0;
                case 5 -> rareEarth.units() * 1000 / RareEarthProgress.UNITS_PER_OUTPUT;
                case 6 -> grinderMode;
                default -> throw new IndexOutOfBoundsException(index);
            };
        }
    };

    AdvancedMachineEntity(BlockPos pos, BlockState state) {
        super(AdvancedMachines.ENTITY.get(), pos, state);
    }

    void tick() {
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
        var step = InductionCycle.step(heat, progress, energy, work, level.hasNeighborSignal(worldPosition), variant.cost, duration);
        boolean changed = step.spent() > 0 || heat != step.heat() || progress != step.progress();
        energy -= step.spent();
        heat = step.heat();
        progress = step.progress();
        processing = step.processing();
        if (step.finished()) {
            for (int pass = 0; pass < variant.layout.processingPasses(); pass++) {
                process(variant.layout.inputForPass(pass, split, inventory.getStackInSlot(1).isEmpty(),
                    variant.layout.supportsSplit() && inventory.getStackInSlot(2).isEmpty()));
            }
        }
        updateLit(step.heating());
        if (changed) setChanged();
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
        var step = RecyclerControl.slowStep(progress, energy, grinderMode, duration, recipe != null);
        boolean changed = step.spent() > 0 || progress != step.progress();
        progress = step.progress();
        energy -= step.spent();
        processing = step.processing();
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
            !input.isEmpty() || level.hasNeighborSignal(worldPosition), variant.cost, duration);
        int nextProgress = work ? step.progress() : 0;
        if (!step.processing() && work) nextProgress = Math.max(0, progress - 300);
        boolean changed = step.spent() > 0 || heat != step.heat() || progress != nextProgress;
        energy -= step.spent();
        heat = step.heat();
        progress = nextProgress;
        processing = step.processing();
        if (processing) rareEarth = selected;
        if (step.finished()) {
            rareEarth = batch.next();
            inventory.extractItem(1, 1, false);
            if (batch.batches() > 0) {
                ItemStack current = inventory.getStackInSlot(2);
                inventory.setStackInSlot(2, output.copyWithCount(current.getCount() + output.getCount()));
            }
        }
        updateLit(step.heating());
        if (changed) setChanged();
    }

    private void updateLit(boolean lit) {
        BlockState state = getBlockState();
        if (state.getValue(BlockStateProperties.LIT) != lit) {
            level.setBlock(worldPosition, state.setValue(BlockStateProperties.LIT, lit), Block.UPDATE_CLIENTS);
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
        experience += recipe.experience();
    }

    private void dischargeBattery() {
        ItemStack stack = inventory.getStackInSlot(0);
        if (!RechargeableItem.supports(stack, PACKET, false)) return;
        RechargeableItem cell = (RechargeableItem) stack.getItem();
        int stored = cell.charge(stack);
        int moved = Math.min(Math.min(stored, cell.transferRate()), variant.capacity() - energy);
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
        return side == Direction.UP ? input : side == Direction.DOWN ? bottom : sides;
    }

    @Override public boolean canReceiveEnergy(Direction side, MachineKind source, int packet) {
        return packet <= PACKET && energy < variant.capacity();
    }

    @Override public int receiveEnergy(int available) {
        int received = Math.min(available, variant.capacity() - energy);
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
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        for (int i = 0; i < variant.layout.inputs(); i++) trackedInputs[i] = inventory.getStackInSlot(i + 1).copyWithCount(1);
        energy = Mth.clamp(tag.getInt("energy"), 0, variant.capacity());
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

    private record ProcessingRecipe(ItemStack output, int duration, float experience, int inputCount, PumpTank pump, int chanceDenominator) {
        ProcessingRecipe(ItemStack output, int duration, float experience, int inputCount, PumpTank pump) {
            this(output, duration, experience, inputCount, pump, 1);
        }
        ProcessingRecipe(ItemStack output, int duration, float experience) { this(output, duration, experience, 1, null); }
    }
}
