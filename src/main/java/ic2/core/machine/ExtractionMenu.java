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
    private final ContainerData data;
    private final int machineSlots, outputSlot;
    ExtractionMenu(int id, Inventory player) { this(id, player, null); }
    ExtractionMenu(int id, Inventory player, ExtractionEntity machine) {
        this(ExtractionMachines.MENU.get(), id, player, machine, machine == null ? new ItemStackHandler(3) : machine.inventory,
            machine == null ? new SimpleContainerData(4) : machine.data, 2);
    }
    ExtractionMenu(net.minecraft.world.inventory.MenuType<?> type, int id, Inventory player, net.minecraft.world.level.block.entity.BlockEntity machine,
            ItemStackHandler inventory, ContainerData values, int outputSlot) {
        super(type, id); this.owner = machine;
        this.machineSlots = inventory.getSlots(); this.outputSlot = outputSlot; data = values;
        addDataSlots(data);
        for (int slot = 0; slot < machineSlots; slot++) {
            final int index = slot;
            addSlot(new SlotItemHandler(inventory, slot, 44 + slot * 36, 35) {
                @Override public boolean mayPlace(ItemStack stack) { return index != outputSlot && super.mayPlace(stack); }
            });
        }
        for (int row = 0; row < 3; row++) for (int column = 0; column < 9; column++) addSlot(new Slot(player, 9 + row * 9 + column, 8 + column * 18, 84 + row * 18));
        for (int column = 0; column < 9; column++) addSlot(new Slot(player, column, 8 + column * 18, 142));
    }
    int value(int index) { return data.get(index); }
    private final net.minecraft.world.level.block.entity.BlockEntity owner;
    @Override public boolean stillValid(Player player) { return owner == null || !owner.isRemoved() && owner.getLevel().getBlockEntity(owner.getBlockPos()) == owner && player.distanceToSqr(owner.getBlockPos().getCenter()) <= 64; }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size() || !slots.get(index).hasItem()) return ItemStack.EMPTY;
        var slot = slots.get(index); var stack = slot.getItem(); var original = stack.copy();
        if (!(index < machineSlots ? moveItemStackTo(stack, machineSlots, slots.size(), true) : moveItemStackTo(stack, 0, outputSlot < 0 ? machineSlots : outputSlot, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged(); slot.onTake(player, stack); return original;
    }
}
