package ic2.core.machine;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

final class ColossalMenu extends AbstractContainerMenu {
    final int lanes;
    final ColossalMachines.Kind kind;
    private final ColossalEntity device;
    private final ContainerData data;

    ColossalMenu(int id, Inventory inventory, int lanes, ColossalMachines.Kind kind) {
        this(id, inventory, lanes, kind, null, new ItemStackHandler(19), new SimpleContainerData(28));
    }
    ColossalMenu(int id, Inventory playerInventory, int lanes, ColossalMachines.Kind kind, ColossalEntity device, ItemStackHandler inventory, ContainerData data) {
        super(ColossalMachines.MENU.get(), id);
        if (lanes != 2 && lanes != 4 && lanes != 8) throw new IllegalArgumentException("Invalid colossal lane count");
        this.lanes = lanes; this.kind = kind; this.device = device; this.data = data; addDataSlots(data);
        for (int lane = 0; lane < lanes; lane++) addSlot(new SlotItemHandler(inventory, lane, inputX(lanes, lane), inputY(lanes, lane)));
        for (int lane = 0; lane < lanes; lane++) {
            addSlot(new SlotItemHandler(inventory, 8 + lane, inputX(lanes, lane) + (lanes == 2 ? 57 : 42), inputY(lanes, lane)) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public void onTake(Player player, ItemStack stack) { super.onTake(player, stack); if (device != null) device.releaseExperience(); }
            });
        }
        int offset = lanes == 8 ? 15 : 0;
        addSlot(new SlotItemHandler(inventory, 16, 116, 82 + offset));
        addSlot(new SlotItemHandler(inventory, 17, 134, 82 + offset));
        addSlot(new SlotItemHandler(inventory, 18, 152, 82 + offset));
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++)
            addSlot(new Slot(playerInventory, 9 + row * 9 + column, 8 + column * 18, 102 + offset + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(playerInventory, column, 8 + column * 18, 160 + offset));
    }
    static int inputX(int lanes, int lane) { return lanes == 2 ? 52 : 28 + lane % 2 * 63; }
    static int inputY(int lanes, int lane) { return lanes == 2 ? 23 + lane * 32 : (lanes == 4 ? 30 : 19) + lane / 2 * 19; }
    int value(int index) { return data.get(index * 2) & 65535 | data.get(index * 2 + 1) << 16; }
    @Override public boolean stillValid(Player player) { return device == null || device.live() && device.lanes() == lanes && player.distanceToSqr(device.getBlockPos().getCenter()) <= 64; }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem(); var before = stack.copy();
        int machineSlots = lanes * 2 + 3;
        boolean moved;
        if (index < machineSlots) moved = moveItemStackTo(stack, machineSlots, slots.size(), true);
        else {
            moved = moveItemStackTo(stack, lanes * 2 + 1, machineSlots, false);
            if (!moved) moved = moveItemStackTo(stack, lanes * 2, lanes * 2 + 1, false);
            if (!moved) moved = moveItemStackTo(stack, 0, lanes, false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, before.copyWithCount(before.getCount() - stack.getCount())); return before;
    }
}
