package ic2.core.machine;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

final class ChargingMenu extends AbstractContainerMenu {
    final ChargingDevices.Kind kind;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final ChargingEntity device;
    ChargingMenu(int id, Inventory player, ChargingDevices.Kind kind) {
        this(id, player, kind, new ItemStackHandler(kind.slots()), new SimpleContainerData(22), ContainerLevelAccess.NULL, null);
    }
    ChargingMenu(int id, Inventory player, ChargingDevices.Kind kind, ItemStackHandler inventory, ContainerData data, ContainerLevelAccess access, ChargingEntity device) {
        super(kind.menu(), id); this.kind = kind; this.data = data; this.access = access; this.device = device; addDataSlots(data);
        for (int slot = 0; slot < kind.slots(); slot++) {
            int x = slot < 16 && kind.inventoryGrid() ? 26 + slot % 4 * 18 : 134;
            int y = slot < 16 && kind.inventoryGrid() ? 43 + slot / 4 * 18 : 70;
            addSlot(new SlotItemHandler(inventory, slot, x, y) { @Override public int getMaxStackSize() { return 1; } });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(player, 9 + row * 9 + col, 8 + col * 18, 160 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, 218));
    }
    int value(int index) { return data.get(index * 2) & 65535 | data.get(index * 2 + 1) << 16; }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (device == null || !stillValid(player) || button < 0 || button > (kind == ChargingDevices.Kind.MASTER ? 12 : 3)) return false;
        device.button(player, button); return true;
    }
    @Override public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).is(kind.block()) && level.getBlockEntity(pos) == device
            && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64, true);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), before = stack.copy();
        boolean moved = index < kind.slots() ? moveItemStackTo(stack, kind.slots(), slots.size(), true)
            : kind.slots() > 0 && moveItemStackTo(stack, 0, kind.slots(), false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, before.copyWithCount(before.getCount() - stack.getCount())); return before;
    }
}
