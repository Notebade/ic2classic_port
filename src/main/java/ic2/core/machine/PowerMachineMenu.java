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
        this(id, player, kind, new ItemStackHandler(kind.slots), new SimpleContainerData(42), ContainerLevelAccess.NULL, null);
    }
    PowerMachineMenu(int id, Inventory player, PowerMachines.Kind kind, ItemStackHandler inventory, ContainerData data,
                     ContainerLevelAccess access, PowerMachineEntity machine) {
        super(kind.menu(), id); this.kind = kind; this.data = data; this.access = access; this.machine = machine;
        addDataSlots(data);
        var layout = GenerationLayout.forId(kind.id);
        for (int slot = 0; slot < kind.slots; slot++) {
            final int index = slot;
            int x = layout == null ? 8 + slot * 22 : layout.slots().get(slot).x();
            int y = layout == null ? 100 : layout.slots().get(slot).y();
            addSlot(new SlotItemHandler(inventory, slot, x, y) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return !kind.outputSlot(index) && super.mayPlace(stack);
                }
            });
        }
        int inventoryY = layout == null ? 132 : layout.inventoryY();
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(player, 9 + row * 9 + col, 8 + col * 18, inventoryY + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, inventoryY + 58));
    }
    int value(int index) { return data.get(index * 2) & 65535 | data.get(index * 2 + 1) << 16; }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (machine == null || !stillValid(player) || button < 0 || button > 10 || !(kind == PowerMachines.Kind.TRANSFORMER || kind.chemical())) return false;
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
        boolean moved = index < kind.slots ? moveItemStackTo(stack, kind.slots, slots.size(), true)
            : kind == PowerMachines.Kind.GASIFIER && stack.is(net.minecraft.tags.ItemTags.LOGS)
                ? moveItemStackTo(stack, 1, 2, false) || moveItemStackTo(stack, 0, kind.slots, false)
                : kind.slots > 0 && moveItemStackTo(stack, 0, kind.slots, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, before.copyWithCount(before.getCount() - stack.getCount()));
        return before;
    }
}
