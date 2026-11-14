package ic2.core.equipment;

import ic2.api.item.ArmorModule;
import ic2.api.item.ModularArmor;
import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Module slots are views of the single stored armor, never a second inventory owning its modules. */
final class ArmorConfiguratorMenu extends AbstractContainerMenu {
    private static final List<ResourceLocation> ORDER = List.of(ArmorModule.BATTERY, ArmorModule.HUD,
        ArmorModule.GENERIC, ArmorModule.CHARGER, ArmorModule.STORAGE, ArmorModule.BACK, ArmorModule.MOVEMENT);
    private final SimpleContainer contents;
    private final SimpleContainer display = new SimpleContainer(27);
    private final DataSlot offset = DataSlot.standalone();
    private final boolean client;

    ArmorConfiguratorMenu(int id, Inventory inventory, SimpleContainer contents) {
        super(ArmorConfiguratorStorage.MENU.get(), id);
        this.contents = contents;
        client = inventory.player.level().isClientSide;
        addDataSlot(offset);
        addSlot(new Slot(contents, 0, 80, 117) {
            @Override public boolean mayPlace(ItemStack stack) { return stack.getItem() instanceof ModularArmor; }
            @Override public int getMaxStackSize() { return 1; }
        });
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) {
            final int cell = row * 9 + column;
            addSlot(new Slot(display, cell, 8 + column * 18, 29 + row * 30) {
                @Override public boolean isActive() { return cell % 9 < rowSize(cell / 9); }
                @Override public int getMaxStackSize() { return 1; }
                @Override public boolean mayPickup(Player player) { return false; }
                @Override public boolean mayPlace(ItemStack stack) {
                    var shell = contents.getItem(0);
                    return isActive() && shell.getItem() instanceof ModularArmor armor
                        && armor.canInstall(shell, category(cell / 9), stack);
                }
            });
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, 140 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 198));
        EquipmentSlot[] equipment = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        for (int i = 0; i < 4; i++) {
            final var equipmentSlot = equipment[i];
            addSlot(new Slot(inventory, 39 - i, 116 + i * 18, 117) {
                @Override public int getMaxStackSize() { return 1; }
                @Override public boolean mayPlace(ItemStack stack) { return stack.canEquip(equipmentSlot, inventory.player); }
                @Override public boolean mayPickup(Player player) {
                    return player.isCreative() || !hasBinding(getItem());
                }
            });
        }
    }

    private static boolean hasBinding(ItemStack stack) {
        return net.minecraft.world.item.enchantment.EnchantmentHelper.has(stack, net.minecraft.world.item.enchantment.EnchantmentEffectComponents.PREVENT_ARMOR_CHANGE);
    }

    private List<ResourceLocation> categories() {
        var shell = contents.getItem(0);
        if (!(shell.getItem() instanceof ModularArmor armor)) return List.of();
        return armor.moduleLimits(shell).entrySet().stream().filter(e -> e.getValue() > 0).map(java.util.Map.Entry::getKey)
            .sorted(java.util.Comparator.<ResourceLocation>comparingInt(id -> ORDER.contains(id) ? ORDER.indexOf(id) : ORDER.size()).thenComparing(id -> id)).toList();
    }
    int maxOffset() { return Math.max(0, categories().size() - 3); }
    int offset() { return offset.get(); }
    ResourceLocation category(int row) {
        var categories = categories(); int index = row + offset();
        return index >= 0 && index < categories.size() ? categories.get(index) : null;
    }
    int rowSize(int row) {
        var shell = contents.getItem(0); var category = category(row);
        return category != null && shell.getItem() instanceof ModularArmor armor ? Math.min(9, armor.moduleLimits(shell).getOrDefault(category, 0)) : 0;
    }
    private int moduleIndex(int cell) {
        var shell = contents.getItem(0);
        if (!(shell.getItem() instanceof ModularArmor armor)) return -1;
        var entries = armor.modules(shell); int column = cell % 9;
        for (int i = 0; i < entries.size(); i++) if (entries.get(i).category().equals(category(cell / 9)) && column-- == 0) return i;
        return -1;
    }
    private void refresh() {
        offset.set(net.minecraft.util.Mth.clamp(offset(), 0, maxOffset()));
        var shell = contents.getItem(0);
        for (int cell = 0; cell < 27; cell++) {
            int index = moduleIndex(cell);
            display.setItem(cell, index < 0 ? ItemStack.EMPTY : ((ModularArmor) shell.getItem()).modules(shell).get(index).stack());
        }
    }
    @Override public void broadcastChanges() { if (!client) refresh(); super.broadcastChanges(); }
    @Override public void broadcastFullState() { if (!client) refresh(); super.broadcastFullState(); }
    @Override public boolean stillValid(Player player) { return contents.stillValid(player); }
    @Override public boolean clickMenuButton(Player player, int id) {
        if (client || !stillValid(player) || id < 0 || id > maxOffset()) return false;
        offset.set(id); broadcastChanges(); return true;
    }
    @Override public boolean canDragTo(Slot slot) { return slot.container != display && super.canDragTo(slot); }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.container != display && super.canTakeItemForPickAll(stack, slot);
    }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {
        if (!stillValid(player)) return;
        if (slot < 1 || slot > 27) { super.clicked(slot, button, type, player); return; }
        if (client || !getSlot(slot).isActive()) return;
        int cell = slot - 1;
        if (type == ClickType.PICKUP && (button == 0 || button == 1)) {
            var result = exchange(cell, getCarried());
            if (result != null) setCarried(result);
        } else if (type == ClickType.SWAP && (button >= 0 && button < 9 || button == 40)) {
            var result = exchange(cell, player.getInventory().getItem(button));
            if (result != null) player.getInventory().setItem(button, result);
        } else if (type == ClickType.THROW && getCarried().isEmpty()) {
            var result = exchange(cell, ItemStack.EMPTY);
            if (result != null && !result.isEmpty()) player.drop(result, true);
        } else if (type == ClickType.CLONE && player.isCreative() && getCarried().isEmpty()) {
            setCarried(getSlot(slot).getItem().copy());
        }
        broadcastChanges();
    }
    /** Validate against a copy, then publish one armor state and one cursor state together. */
    private ItemStack exchange(int cell, ItemStack carried) {
        var shell = contents.getItem(0).copy();
        if (!(shell.getItem() instanceof ModularArmor armor)) return null;
        int index = moduleIndex(cell);
        if (index >= 0 && carried.getCount() > 1) return null;
        var removed = index < 0 ? ItemStack.EMPTY : armor.removeModule(shell, index, false);
        if (index >= 0 && removed.isEmpty()) return null;
        var remainder = carried.copy();
        if (!remainder.isEmpty() && !armor.install(shell, category(cell / 9), remainder)) return null;
        contents.setItem(0, shell);
        return removed.isEmpty() ? remainder : removed;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size() || index >= 1 && index <= 27) return ItemStack.EMPTY;
        var slot = slots.get(index); var stack = slot.getItem();
        if (stack.isEmpty() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var previous = stack.copy();
        boolean moved;
        if (index == 0 || index >= 64) moved = moveItemStackTo(stack, 28, 64, true);
        else if (stack.getItem() instanceof ModularArmor && !getSlot(0).hasItem()) moved = moveItemStackTo(stack, 0, 1, false);
        else if (moveItemStackTo(stack, 64, 68, false)) moved = true;
        else moved = index < 55 ? moveItemStackTo(stack, 55, 64, false) : moveItemStackTo(stack, 28, 55, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, stack); return previous;
    }
}
