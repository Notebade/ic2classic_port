package ic2.core.machine;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

final class ToolboxMenu extends AbstractContainerMenu {
    private final Container contents;

    ToolboxMenu(int id, Inventory player, Container contents, ItemStack box) {
        super(MenuType.GENERIC_3x3, id);
        this.contents = contents;
        for (int i = 0; i < 9; i++) addSlot(new Slot(contents, i, 62 + i % 3 * 18, 17 + i / 3 * 18){

            @Override
            public boolean mayPlace(ItemStack stack) {
                return contents.canPlaceItem(getContainerSlot(), stack);
            }

            @Override
            public boolean mayPickup(Player owner) {
                return getContainerSlot() < 8;
            }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addPlayerSlot(player, box, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18);
        for (int col = 0; col < 9; col++) addPlayerSlot(player, box, col, 8 + col * 18, 142);
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
        if (!(index < 9 ? moveItemStackTo(stack, 9, slots.size(), true) : moveItemStackTo(stack, 0, 8, false))) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, before.copyWithCount(before.getCount() - stack.getCount()));
        return before;
    }
}
