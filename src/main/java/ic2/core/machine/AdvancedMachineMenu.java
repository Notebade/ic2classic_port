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

final class AdvancedMachineMenu extends AbstractContainerMenu {
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final AdvancedMachineEntity machine;
    final AdvancedMachines.Variant variant;

    AdvancedMachineMenu(int id, Inventory player) {
        this(id, player, AdvancedMachines.Variant.FURNACE);
    }

    AdvancedMachineMenu(int id, Inventory player, AdvancedMachines.Variant variant) {
        this(id, player, new ItemStackHandler(variant.layout.slots()), new SimpleContainerData(5), ContainerLevelAccess.NULL, null, variant);
    }

    AdvancedMachineMenu(int id, Inventory player, ItemStackHandler inventory, ContainerData data,
                  ContainerLevelAccess access, AdvancedMachineEntity machine, AdvancedMachines.Variant variant) {
        super(variant.menu(), id);
        this.variant = variant;
        this.data = data;
        this.access = access;
        this.machine = machine;
        addDataSlots(data);
        addSlot(new SlotItemHandler(inventory, 0, 26, 53));
        addSlot(new SlotItemHandler(inventory, 1, 47, 17));
        if (variant.layout.supportsSplit()) addSlot(new SlotItemHandler(inventory, 2, 65, 17));
        int firstOutput = variant.layout.inputs() + 1;
        for (int index = firstOutput; index < variant.layout.slots(); index++) {
            addSlot(new SlotItemHandler(inventory, index, 95 + (index - firstOutput) * 18, 35) {
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public void onTake(Player owner, ItemStack stack) {
                    if (machine != null) machine.awardExperience(owner.position());
                    stack.onCraftedBy(owner.level(), owner, stack.getCount());
                    super.onTake(owner, stack);
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(player, 9 + row * 9 + col, 8 + col * 18, 84 + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 8 + col * 18, 142));
    }

    int energy() { return data.get(0); }
    int heat() { return data.get(1); }
    int progress() { return data.get(2); }
    boolean split() { return data.get(3) != 0; }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (id != 0 || !variant.layout.supportsSplit() || !stillValid(player)) return false;
        if (machine != null) machine.toggleSplit();
        return true;
    }

    @Override public boolean stillValid(Player player) {
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof AdvancedMachineBlock
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64, true);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        int machineSlots = variant.layout.slots();
        boolean moved = index < machineSlots ? moveItemStackTo(stack, machineSlots, slots.size(), true)
            : moveItemStackTo(stack, 0, 1, false) || moveItemStackTo(stack, 1, variant.layout.inputs() + 1, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, before.copyWithCount(before.getCount() - stack.getCount()));
        return before;
    }
}
