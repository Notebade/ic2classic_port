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

final class ExtractionMenu extends AbstractContainerMenu {
    private final ExtractionEntity machine;
    private final ContainerData data;
    ExtractionMenu(int id, Inventory player) { this(id, player, null); }
    ExtractionMenu(int id, Inventory player, ExtractionEntity machine) {
        super(ExtractionMachines.MENU.get(), id); this.machine = machine;
        var inventory = machine == null ? new ItemStackHandler(3) : machine.inventory;
        data = machine == null ? new SimpleContainerData(4) : machine.data;
        addDataSlots(data);
        for (int slot = 0; slot < 3; slot++) {
            final int index = slot;
            addSlot(new SlotItemHandler(inventory, slot, 44 + slot * 36, 35) {
                @Override public boolean mayPlace(ItemStack stack) { return index != 2 && super.mayPlace(stack); }
            });
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(player, 9 + row * 9 + column, 8 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(player, column, 8 + column * 18, 142));
    }
    int value(int index) { return data.get(index); }
    @Override public boolean stillValid(Player player) { return machine == null || !machine.isRemoved() && machine.getLevel().getBlockEntity(machine.getBlockPos()) == machine && player.distanceToSqr(machine.getBlockPos().getCenter()) <= 64; }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !slots.get(index).hasItem()) return ItemStack.EMPTY;
        var slot = slots.get(index); var stack = slot.getItem(); var original = stack.copy();
        if (!(index < 3 ? moveItemStackTo(stack, 3, slots.size(), true) : moveItemStackTo(stack, 0, 2, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged(); slot.onTake(player, stack); return original;
    }
}
