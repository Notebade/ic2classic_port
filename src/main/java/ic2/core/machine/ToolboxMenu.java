package ic2.core.machine;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class ToolboxMenu extends AbstractContainerMenu {
    private final Container contents;
    private final int capacity;
    private final int physical;

    ToolboxMenu(int id, Inventory player, Container contents, ItemStack box, int capacity) {
        super(capacity == 8 ? StorageInfrastructure.TOOLBOX_MENU.get() : capacity == 15 ? StorageInfrastructure.CARBON_MENU.get() : StorageInfrastructure.IRIDIUM_MENU.get(), id);
        this.contents = contents;
        this.capacity = capacity; this.physical = contents.getContainerSize();
        int columns = capacity == 8 ? 3 : 9;
        for (int i = 0; i < physical; i++) addSlot(new Slot(contents, i, (capacity == 8 ? 62 : 8) + i % columns * 18, (capacity == 8 ? 17 : 18) + i / columns * 18){

            @Override
            public boolean mayPlace(ItemStack stack) {
                return getContainerSlot() < capacity && contents.canPlaceItem(getContainerSlot(), stack);
            }

            @Override public boolean isActive() { return getContainerSlot() < capacity; }

            @Override
            public boolean mayPickup(Player owner) {
                return getContainerSlot() < capacity;
            }
        });
        int inventoryY = capacity == 8 ? 84 : physical / 9 * 18 + 31;
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addPlayerSlot(player, box, 9 + row * 9 + col, 8 + col * 18, inventoryY + row * 18);
        for (int col = 0; col < 9; col++) addPlayerSlot(player, box, col, 8 + col * 18, inventoryY + 58);
    }

    private void addPlayerSlot(Inventory player, ItemStack box, int index, int x, int y) {
        addSlot(new Slot(player, index, x, y){

            @Override
            public boolean mayPickup(Player owner) {
                return getItem() != box;
            }

            @Override
            public boolean mayPlace(ItemStack stack) {
                return getItem() != box;
            }
        });
    }

    @Override
    public boolean stillValid(Player player) {
        return contents.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        var slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        var stack = slot.getItem();
        var before = stack.copy();
        if (!(index < physical ? moveItemStackTo(stack, physical, slots.size(), true) : moveItemStackTo(stack, 0, capacity, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, before.copyWithCount(before.getCount() - stack.getCount()));
        return before;
    }
}
