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

final class PowerMachineMenu extends AbstractContainerMenu {
    final PowerMachines.Kind kind;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final PowerMachineEntity machine;
    PowerMachineMenu(int id, Inventory player, PowerMachines.Kind kind) {
        this(id, player, kind, new ItemStackHandler(kind.slots), new SimpleContainerData(34), ContainerLevelAccess.NULL, null);
    }
    PowerMachineMenu(int id, Inventory player, PowerMachines.Kind kind, ItemStackHandler inventory, ContainerData data,
                     ContainerLevelAccess access, PowerMachineEntity machine) {
        super(kind.menu(), id); this.kind = kind; this.data = data; this.access = access; this.machine = machine;
        addDataSlots(data);
        for (int slot = 0; slot < kind.slots; slot++) {
            final int index = slot;
            addSlot(new SlotItemHandler(inventory, slot, 8 + slot * 22, 100) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return !((kind == PowerMachines.Kind.PLASMAFIER || kind == PowerMachines.Kind.LIQUID_FUEL) && index == 2 || kind == PowerMachines.Kind.BOILER && (index == 1 || index == 3))
                        && super.mayPlace(stack);
                }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(player, 9 + row * 9 + col, 8 + col * 18, 132 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, 190));
    }
    int value(int index) { return data.get(index * 2) & 65535 | data.get(index * 2 + 1) << 16; }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (machine == null || !stillValid(player) || kind != PowerMachines.Kind.TRANSFORMER || button < 0 || button > 5) return false;
        machine.button(button); return true;
    }
    @Override public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).is(kind.block())
            && (machine == null || level.getBlockEntity(pos) == machine)
            && player.distanceToSqr(pos.getX() + .5, pos.getY() + .5, pos.getZ() + .5) <= 64, true);
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(), before = stack.copy();
        if (!(index < kind.slots ? moveItemStackTo(stack, kind.slots, slots.size(), true) : kind.slots > 0 && moveItemStackTo(stack, 0, kind.slots, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, before.copyWithCount(before.getCount() - stack.getCount()));
        return before;
    }
}
