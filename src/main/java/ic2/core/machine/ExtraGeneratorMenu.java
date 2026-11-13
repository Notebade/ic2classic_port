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

final class ExtraGeneratorMenu extends AbstractContainerMenu {
    final ExtraGenerators.Type type;
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final ExtraGeneratorEntity generator;

    ExtraGeneratorMenu(int id, Inventory player, ExtraGenerators.Type type) {
        this(id, player, type, new ItemStackHandler(3), new SimpleContainerData(6), ContainerLevelAccess.NULL, null);
    }

    ExtraGeneratorMenu(int id, Inventory player, ExtraGenerators.Type type, ItemStackHandler inventory, ContainerData data,
                       ContainerLevelAccess access, ExtraGeneratorEntity generator) {
        super(type.menu(), id);
        this.type = type; this.data = data; this.access = access; this.generator = generator;
        addDataSlots(data);
        addSlot(new SlotItemHandler(inventory, 0, 26, 76));
        addSlot(new SlotItemHandler(inventory, 1, 80, 76));
        addSlot(new SlotItemHandler(inventory, 2, 134, 76) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(player, 9 + row * 9 + col, 8 + col * 18, 106 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, 164));
    }

    int energy() { return data.get(0); }
    int heat() { return data.get(1); }
    int water() { return (data.get(3) << 16) | (data.get(2) & 0xffff); }
    int fuelPercent() { return data.get(4) / 10; }
    int outputHundredths() { return data.get(5); }

    @Override public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof ExtraGeneratorBlock block
            && block.type == type && (generator == null || level.getBlockEntity(pos) == generator)
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64, true);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem(); ItemStack before = stack.copy();
        boolean moved = index < 3 ? moveItemStackTo(stack, 3, slots.size(), true)
            : moveItemStackTo(stack, 0, 1, false) || moveItemStackTo(stack, 1, 2, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, before.copyWithCount(before.getCount() - stack.getCount()));
        return before;
    }
}
