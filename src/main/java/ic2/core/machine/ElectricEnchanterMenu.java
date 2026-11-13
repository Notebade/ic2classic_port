package ic2.core.machine;

import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

final class ElectricEnchanterMenu extends AbstractContainerMenu {
    private final ElectricEnchanterEntity device;
    private final ContainerData data;
    private final Player player;
    private final SimpleContainer preview = new SimpleContainer(1);

    ElectricEnchanterMenu(int id, Inventory inventory) { this(id, inventory, null, new ItemStackHandler(7), new SimpleContainerData(20)); }
    ElectricEnchanterMenu(int id, Inventory inventory, ElectricEnchanterEntity device, ItemStackHandler items, ContainerData data) {
        super(ElectricEnchanting.MENU.get(), id); this.device = device; this.data = data; player = inventory.player; addDataSlots(data);
        int[] x = {13,42,46,64,82,64,126}; int[] y = {39,39,16,16,16,39,39};
        for (int slot = 0; slot < 7; slot++) {
            if (slot == 6) addSlot(new SlotItemHandler(items, slot, x[slot], y[slot]) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public void onTake(Player player, ItemStack stack) { super.onTake(player, stack); player.awardStat(net.minecraft.stats.Stats.ENCHANT_ITEM); }
            });
            else addSlot(new SlotItemHandler(items, slot, x[slot], y[slot]));
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, 9 + row * 9 + column, 8 + column * 18, 90 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(inventory, column, 8 + column * 18, 148));
        addSlot(new Slot(preview, 0, -10000, -10000) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
            @Override public boolean mayPickup(Player player) { return false; }
            @Override public boolean isActive() { return false; }
        });
    }
    int value(int index) { return data.get(index * 2) & 65535 | data.get(index * 2 + 1) << 16; }
    ItemStack preview() { return preview.getItem(0); }
    @Override public void broadcastChanges() {
        if (device != null) preview.setItem(0, device.preview(player.getEnchantmentSeed()));
        super.broadcastChanges();
    }
    @Override public boolean stillValid(Player player) { return device == null || !device.isRemoved() && device.getLevel().getBlockEntity(device.getBlockPos()) == device && player.distanceToSqr(device.getBlockPos().getCenter()) <= 64; }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (device == null || !stillValid(player) || button < 0 || button > 7) return false;
        device.button(player, button); return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= 43) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem(); var before = stack.copy();
        boolean moved;
        if (index < 7) moved = moveItemStackTo(stack, 7, 43, true);
        else {
            moved = moveItemStackTo(stack, 1, 5, false);
            if (!moved) moved = moveItemStackTo(stack, 5, 6, false);
            if (!moved) moved = moveItemStackTo(stack, 0, 1, false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, before.copyWithCount(before.getCount() - stack.getCount())); return before;
    }
}
