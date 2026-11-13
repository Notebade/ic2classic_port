package ic2.core.machine;

import ic2.api.machine.MachineSnapshot;
import ic2.core.item.RechargeableItem;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

final class ElectricEnchanterEntity extends BlockEntity implements MenuProvider, EnergySink, MachineSnapshot.Provider {
    private int energy, mode, experience, needed, enchantLevel, progress, seed;
    private boolean running, valid, dirty = true, mutating;
    final ItemStackHandler inventory = new ItemStackHandler(7) {
        @Override public int getSlotLimit(int slot) { return slot == 5 ? 1 : 64; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            return switch (slot) {
                case 0 -> RechargeableItem.supports(stack.copyWithCount(1), 512, false) || stack.is(Items.REDSTONE) || stack.is(Items.REDSTONE_BLOCK);
                case 1 -> stack.is(Items.LAPIS_LAZULI);
                case 2, 3, 4 -> ElectricEnchantments.book(stack);
                case 5 -> ElectricEnchantments.target(stack);
                default -> false;
            };
        }
        @Override protected void onContentsChanged(int slot) {
            if (!mutating && slot != 0) { dirty = true; running = false; progress = 0; }
            setChanged();
        }
    };
    final ContainerData data = new ContainerData() {
        @Override public int getCount() { return 20; }
        @Override public int get(int index) {
            int value = switch (index / 2) { case 0 -> energy; case 1 -> mode; case 2 -> experience; case 3 -> needed; case 4 -> enchantLevel;
                case 5 -> running ? 1 : 0; case 6 -> progress; case 7 -> mode == 2 ? 250 : 100; case 8 -> valid ? 1 : 0; default -> seed; };
            return index % 2 == 0 ? value & 65535 : value >>> 16;
        }
        @Override public void set(int index, int value) {}
    };
    ElectricEnchanterEntity(BlockPos pos, BlockState state) { super(ElectricEnchanting.ENTITY.get(), pos, state); }

    private List<ItemStack> books() {
        var result = new ArrayList<ItemStack>();
        for (int slot = 2; slot < 5; slot++) if (!inventory.getStackInSlot(slot).isEmpty()) result.add(inventory.getStackInSlot(slot));
        return List.copyOf(result);
    }
    private int bookCount() {
        int count = 0;
        for (var stack : books()) {
            boolean accepted = switch (mode) { case 1 -> stack.is(Items.ENCHANTED_BOOK); case 2 -> stack.is(Items.BOOK); default -> ElectricEnchantments.book(stack); };
            if (!accepted) return -1;
            count++;
        }
        return count;
    }
    private int shelves() {
        int total = 0;
        for (int y = 0; y < 2; y++) for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++) {
            var pos = worldPosition.offset(x, y, z);
            if (level.hasChunkAt(pos)) total = Math.min(30, total + Math.clamp((int) level.getBlockState(pos).getEnchantPowerBonus(level, pos), 0, 30));
        }
        return total;
    }
    private void refresh() {
        dirty = false;
        var target = inventory.getStackInSlot(5);
        valid = inventory.getStackInSlot(6).isEmpty() && bookCount() > 0 && ElectricEnchantments.target(target);
        enchantLevel = 0; needed = 0;
        if (!valid) return;
        if (mode == 0) {
            enchantLevel = EnchanterRules.enchantLevel(shelves(), inventory.getStackInSlot(1).getCount(), bookCount());
            valid = enchantLevel > 0;
            needed = valid ? EnchanterRules.enchantXp(enchantLevel, target.is(Items.ENCHANTED_BOOK) || target.isEnchanted()) : 0;
        } else if (mode == 1) {
            long count = ElectricEnchantments.count(target);
            for (var book : books()) count += ElectricEnchantments.count(book);
            needed = (int) Math.clamp(count * 10, 0, Integer.MAX_VALUE);
            valid = needed > 0;
        } else {
            valid = !ElectricEnchantments.enchantments(target).isEmpty();
            needed = valid ? 50 : 0;
        }
    }
    void tick() {
        discharge();
        if (dirty || !running && level.getGameTime() % 128 == 0) refresh();
        if (!valid || needed > experience || !inventory.getStackInSlot(6).isEmpty()) { running = false; progress = 0; }
        boolean active = running && energy >= 500;
        if (active) {
            energy -= 500; progress++;
            if (progress >= (mode == 2 ? 250 : 100)) finish();
            setChanged();
        }
        if (getBlockState().getValue(BlockStateProperties.LIT) != active)
            level.setBlock(worldPosition, getBlockState().setValue(BlockStateProperties.LIT, active), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
    }
    private void discharge() {
        var stack = inventory.getStackInSlot(0);
        int room = 900000 - energy;
        if (room <= 0) return;
        if (RechargeableItem.supports(stack, 512, false)) {
            var cell = (ic2.api.item.ElectricItem) stack.getItem();
            int amount = Math.min(room, Math.min(cell.transferRate(stack), cell.charge(stack)));
            if (amount > 0) { cell.setCharge(stack, cell.charge(stack) - amount); energy += amount; setChanged(); }
        } else {
            int value = stack.is(Items.REDSTONE) ? 500 : stack.is(Items.REDSTONE_BLOCK) ? 4500 : 0;
            if (value > 0 && room >= value) { inventory.extractItem(0, 1, false); energy += value; setChanged(); }
        }
    }
    ItemStack preview(int playerSeed) {
        if (dirty) refresh();
        if (!valid) return ItemStack.EMPTY;
        var target = inventory.getStackInSlot(5);
        int selectedSeed = running ? seed : playerSeed;
        return switch (mode) {
            case 1 -> ElectricEnchantments.anvil(target, books(), selectedSeed);
            case 2 -> ElectricEnchantments.removeFirst(target).book();
            default -> ElectricEnchantments.enchant((ServerLevel) level, target, books(), enchantLevel, selectedSeed);
        };
    }
    private void finish() {
        int cost = needed, count = bookCount();
        var result = preview(seed);
        if (result.isEmpty()) { running = false; progress = 0; return; }
        mutating = true;
        try {
            if (mode == 2) {
                inventory.setStackInSlot(5, ElectricEnchantments.removeFirst(inventory.getStackInSlot(5)).remainder());
                for (int slot = 2; slot < 5; slot++) if (!inventory.getStackInSlot(slot).isEmpty()) { inventory.extractItem(slot, 1, false); break; }
            } else {
                inventory.extractItem(5, 1, false);
                if (mode == 0) inventory.extractItem(1, count, false);
                for (int slot = 2; slot < 5; slot++) inventory.extractItem(slot, 1, false);
            }
            inventory.setStackInSlot(6, result);
            experience -= cost; progress = 0; running = false; valid = false; needed = 0; dirty = true;
        } finally { mutating = false; }
        setChanged();
    }
    void button(Player player, int button) {
        if (button < 0 || button > 7) return;
        if (button == 0) { mode = (mode + 1) % 3; running = false; progress = 0; dirty = true; }
        else if (button <= 3) deposit(player, button == 1 ? 1 : button == 2 ? 10 : 100);
        else if (button <= 6 && !running) {
            int amount = Math.min(experience, button == 4 ? 1 : button == 5 ? 10 : 100);
            if (!player.isCreative()) player.giveExperiencePoints(amount);
            experience -= amount; player.onEnchantmentPerformed(ItemStack.EMPTY, 0);
        } else if (button == 7 && !running) {
            refresh();
            if (valid && experience >= needed && needed > 0) {
                seed = player.getEnchantmentSeed(); running = true;
                if (mode == 0) player.onEnchantmentPerformed(ItemStack.EMPTY, 0);
            }
        }
        setChanged();
    }
    private void deposit(Player player, int requested) {
        int room = Math.min(1000 - experience, requested);
        if (room <= 0) return;
        if (player.isCreative()) { experience += room; player.onEnchantmentPerformed(ItemStack.EMPTY, 0); return; }
        int total = (int) Math.min(Integer.MAX_VALUE, (long) EnchanterRules.xpForLevel(player.experienceLevel) + Math.round(player.experienceProgress * player.getXpNeededForNextLevel()));
        int amount = Math.min(room, total), remaining = total - amount;
        player.totalExperience = remaining; player.experienceLevel = EnchanterRules.levelForXp(remaining);
        player.experienceProgress = (remaining - EnchanterRules.xpForLevel(player.experienceLevel)) / (float) player.getXpNeededForNextLevel();
        player.onEnchantmentPerformed(ItemStack.EMPTY, 0); experience += amount;
    }
    private boolean live() { return level != null && !isRemoved() && level.hasChunkAt(worldPosition) && level.getBlockEntity(worldPosition) == this; }
    IItemHandler automation(Direction side) { return GuardedCapabilities.items(() -> live() ? rawAutomation(side) : null); }
    private IItemHandler rawAutomation(Direction side) {
        return side == Direction.DOWN ? new RangedWrapper(inventory, 6, 7) : new RangedWrapper(inventory, 0, 6) {
            @Override public ItemStack extractItem(int slot, int amount, boolean simulate) { return ItemStack.EMPTY; }
        };
    }
    @Override public boolean canReceiveEnergy(Direction side, int sourceStorageOutput, int packet) { return live() && packet > 0 && packet <= 512 && energy < 1000000; }
    @Override public int receiveEnergy(int offered) { if (!live()) return 0; int accepted = Math.clamp(offered, 0, Math.min(512, 1000000 - energy)); energy += accepted; if (accepted > 0) setChanged(); return accepted; }
    @Override public Component getDisplayName() { return Component.translatable("block.ic2.electric_enchanter"); }
    @Override public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) { return new ElectricEnchanterMenu(id, inventory, this, this.inventory, data); }
    void dropContents() { if (!level.isClientSide) for (int slot = 0; slot < 7; slot++) Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), inventory.getStackInSlot(slot)); }
    @Override public MachineSnapshot machineInfo() {
        return MachineTelemetry.snapshot(this, energy, 1000000, MachineSnapshot.EnergyUnit.EU, Map.of("mode", (long) mode, "stored_xp", (long) experience,
            "required_xp", (long) needed, "enchant_level", (long) enchantLevel, "progress_units", (long) progress, "progress_target", mode == 2 ? 250L : 100L,
            "running", running ? 1L : 0L, "valid", valid ? 1L : 0L, "input_packet_eu", 512L, "cost_eu", 500L));
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.saveAdditional(tag, lookup); tag.put("inventory", inventory.serializeNBT(lookup)); tag.putInt("energy", energy); tag.putInt("mode", mode);
        tag.putInt("stored_xp", experience); tag.putInt("progress", progress); tag.putBoolean("running", running); tag.putInt("seed", seed);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider lookup) {
        super.loadAdditional(tag, lookup); mutating = true;
        try { var stored = tag.getCompound("inventory").copy(); stored.putInt("Size", 7); inventory.deserializeNBT(lookup, stored); } finally { mutating = false; }
        energy = Math.clamp(tag.getInt("energy"), 0, 1000000); mode = Math.clamp(tag.getInt("mode"), 0, 2); experience = Math.clamp(tag.getInt("stored_xp"), 0, 1000);
        progress = Math.clamp(tag.getInt("progress"), 0, mode == 2 ? 249 : 99); running = tag.getBoolean("running"); seed = tag.getInt("seed"); dirty = true;
    }
}
