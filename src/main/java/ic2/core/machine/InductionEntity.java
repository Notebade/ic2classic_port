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

final class InductionEntity extends BlockEntity implements MenuProvider, EnergySink {
    static final int CAPACITY = 10000;
    static final int PACKET = 128;
    private final InductionMachines.Variant variant;
    private int energy;
    private int heat;
    private int progress;
    private int duration = 120000;
    private boolean split = true;
    private boolean processing;
    private float experience;
    private final ItemStack[] trackedInputs = {ItemStack.EMPTY, ItemStack.EMPTY};
    final ItemStackHandler inventory = new ItemStackHandler(5) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == 0 ? RechargeableItem.supports(stack, PACKET, false)
                : slot < 3 && recipe(stack) != null;
        }

        @Override
        protected void onContentsChanged(int slot) {
            if (slot == 1 || slot == 2) {
                ItemStack next = getStackInSlot(slot);
                if (!ItemStack.isSameItemSameComponents(trackedInputs[slot - 1], next)) progress = 0;
                trackedInputs[slot - 1] = next.copyWithCount(1);
            }
            setChanged();
        }
    };
    private final RangedWrapper input = new RangedWrapper(inventory, 1, 3) {
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
    };
    private final RangedWrapper output = new RangedWrapper(inventory, 3, 5);
    private final BatterySlot battery = new BatterySlot(inventory, 0, false);
    private final IItemHandler bottom = new CombinedInvWrapper(output, battery);
    private final IItemHandler sides = new CombinedInvWrapper(battery, input, output);
    final ContainerData data = new ContainerData() {
        @Override public int getCount() { return 5; }
        @Override public void set(int index, int value) {}
        @Override public int get(int index) {
            return switch (index) {
                case 0 -> energy;
                case 1 -> heat;
                case 2 -> (int) (1000L * progress / duration);
                case 3 -> split ? 1 : 0;
                case 4 -> processing ? 1 : 0;
                default -> throw new IndexOutOfBoundsException(index);
            };
        }
    };

    InductionEntity(BlockPos pos, BlockState state) {
        super(InductionMachines.ENTITY.get(), pos, state);
        variant = ((InductionBlock) state.getBlock()).variant;
    }

    void tick() {
        dischargeBattery();
        Cooking first = availableRecipe(1);
        Cooking second = availableRecipe(2);
        boolean work = first != null || second != null;
        if (work) duration = Math.max(first == null ? 0 : first.duration(), second == null ? 0 : second.duration());
        var step = InductionCycle.step(heat, progress, energy, work, level.hasNeighborSignal(worldPosition), variant.cost, duration);
        boolean changed = step.spent() > 0 || heat != step.heat() || progress != step.progress();
        energy -= step.spent();
        heat = step.heat();
        progress = step.progress();
        processing = step.processing();
        if (step.finished()) {
            process(split || !inventory.getStackInSlot(1).isEmpty() ? 1 : 2);
            process(split || !inventory.getStackInSlot(2).isEmpty() ? 2 : 1);
        }
        BlockState state = getBlockState();
        if (state.getValue(BlockStateProperties.LIT) != step.heating()) {
            level.setBlock(worldPosition, state.setValue(BlockStateProperties.LIT, step.heating()), Block.UPDATE_CLIENTS);
        }
        if (changed) setChanged();
    }

    private Cooking recipe(ItemStack stack) {
        if (level == null || stack.isEmpty()) return null;
        var input = new SingleRecipeInput(stack);
        return level.getRecipeManager().getRecipeFor(variant.recipes, input, level).map(holder -> {
            var recipe = holder.value();
            int time = (int) Math.min(Integer.MAX_VALUE, Math.max(1L, recipe.getCookingTime()) * 600L);
            return new Cooking(recipe.assemble(input, level.registryAccess()), time, recipe.getExperience());
        }).orElse(null);
    }

    private Cooking availableRecipe(int slot) {
        Cooking recipe = recipe(inventory.getStackInSlot(slot));
        return recipe != null && !recipe.output().isEmpty()
            && room(slot, recipe.output()) >= recipe.output().getCount() ? recipe : null;
    }

    private int room(int inputSlot, ItemStack result) {
        if (result.isEmpty()) return 0;
        int room = 0;
        for (int slot = split ? inputSlot + 2 : 3; slot <= (split ? inputSlot + 2 : 4); slot++) {
            ItemStack current = inventory.getStackInSlot(slot);
            if (current.isEmpty() || ItemStack.isSameItemSameComponents(current, result)) {
                room += Math.min(inventory.getSlotLimit(slot), result.getMaxStackSize()) - current.getCount();
            }
        }
        return room;
    }

    private void process(int inputSlot) {
        Cooking recipe = availableRecipe(inputSlot);
        if (recipe == null) return;
        ItemStack result = recipe.output().copy();
        for (int slot = split ? inputSlot + 2 : 3; slot <= (split ? inputSlot + 2 : 4); slot++) {
            ItemStack current = inventory.getStackInSlot(slot);
            if (!current.isEmpty() && !ItemStack.isSameItemSameComponents(current, result)) continue;
            int moved = Math.min(result.getCount(), Math.min(inventory.getSlotLimit(slot), result.getMaxStackSize()) - current.getCount());
            if (moved > 0) {
                inventory.setStackInSlot(slot, result.copyWithCount(current.getCount() + moved));
                result.shrink(moved);
            }
        }
        inventory.extractItem(inputSlot, 1, false);
        experience += recipe.experience();
    }

    private void dischargeBattery() {
        ItemStack stack = inventory.getStackInSlot(0);
        if (!RechargeableItem.supports(stack, PACKET, false)) return;
        RechargeableItem cell = (RechargeableItem) stack.getItem();
        int stored = cell.charge(stack);
        int moved = Math.min(Math.min(stored, cell.transferRate()), CAPACITY - energy);
        if (moved > 0) {
            ItemStack updated = stack.copy();
            cell.setCharge(updated, stored - moved);
            energy += moved;
            inventory.setStackInSlot(0, updated);
        }
    }

    void toggleSplit() {
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
        return packet <= PACKET && energy < CAPACITY;
    }

    @Override public int receiveEnergy(int available) {
        int received = Math.min(available, CAPACITY - energy);
        energy += received;
        if (received > 0) setChanged();
        return received;
    }

    @Override public Component getDisplayName() { return getBlockState().getBlock().getName(); }

    @Override public AbstractContainerMenu createMenu(int id, Inventory player, Player owner) {
        return new InductionMenu(id, player, inventory, data, ContainerLevelAccess.create(level, worldPosition), this);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("energy", energy);
        tag.putInt("heat", heat);
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        tag.putBoolean("split", split);
        tag.putFloat("experience", experience);
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        for (int i = 0; i < 2; i++) trackedInputs[i] = inventory.getStackInSlot(i + 1).copyWithCount(1);
        energy = Mth.clamp(tag.getInt("energy"), 0, CAPACITY);
        heat = Mth.clamp(tag.getInt("heat"), 0, InductionCycle.MAX_HEAT);
        duration = Math.max(1, tag.getInt("duration"));
        progress = Mth.clamp(tag.getInt("progress"), 0, duration - 1);
        split = !tag.contains("split") || tag.getBoolean("split");
        experience = Math.max(0, tag.getFloat("experience"));
    }

    private record Cooking(ItemStack output, int duration, float experience) {}
}
