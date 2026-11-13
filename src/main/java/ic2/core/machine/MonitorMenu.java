package ic2.core.machine;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

final class MonitorMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final MonitorEntity entity;

    MonitorMenu(int id, Inventory player) {
        this(id, player, new ItemStackHandler(8), new SimpleContainerData(2), ContainerLevelAccess.NULL, null);
    }

    MonitorMenu(int id, Inventory player, ItemStackHandler items, ContainerData data, ContainerLevelAccess access, MonitorEntity entity) {
        super(NuclearInfrastructure.MONITOR_MENU.get(), id);
        this.data = data;
        this.access = access;
        this.entity = entity;
        addDataSlots(data);
        for (int i = 0; i < 8; i++) addSlot(new SlotItemHandler(items, i, 17 + i % 4 * 36, 40 + i / 4 * 20));
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(player, 9 + row * 9 + col, 8 + col * 18, 103 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, 161));
    }

    int energy() {
        return data.get(0);
    }

    boolean redstone() {
        return data.get(1) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return access.evaluate((l,p)->l.getBlockEntity(p) == entity && player.distanceToSqr(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5) <= 64, true);
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (id != 0 || entity == null || !stillValid(p)) return false;
        entity.toggleRedstone();
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var before = stack.copy();
        if (!(index < 8 ? moveItemStackTo(stack, 8, slots.size(), true) : moveItemStackTo(stack, 0, 8, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(p, before.copyWithCount(before.getCount() - stack.getCount()));
        return before;
    }
}
