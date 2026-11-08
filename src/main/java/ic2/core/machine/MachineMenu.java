package ic2.core.machine;

import ic2.core.item.ReBatteryItem;
import java.util.function.IntSupplier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

final class MachineMenu extends AbstractContainerMenu {
    private final ContainerLevelAccess access;
    private final Block block;
    private final DataSlot energy;
    private final DataSlot work;
    private final boolean storage;

    MachineMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, false);
    }

    static MachineMenu batbox(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, true);
    }

    private MachineMenu(int id, Inventory playerInventory, boolean storage) {
        this(id, playerInventory, new ItemStackHandler(MachineEntity.SLOT_COUNT),
            ContainerLevelAccess.NULL, Blocks.AIR, DataSlot.standalone(), DataSlot.standalone(), () -> {}, storage);
    }

    MachineMenu(int id, Inventory playerInventory, ItemStackHandler inventory, ContainerLevelAccess access,
                Block block, IntSupplier energy, IntSupplier work, Runnable changed, boolean storage) {
        this(id, playerInventory, inventory, access, block, readOnly(energy), readOnly(work), changed, storage);
    }

    private MachineMenu(int id, Inventory playerInventory, ItemStackHandler inventory, ContainerLevelAccess access,
                        Block block, DataSlot energy, DataSlot work, Runnable changed, boolean storage) {
        super(storage ? Machines.BATBOX_MENU.get() : Machines.MENU.get(), id);
        this.storage = storage;
        this.access = access;
        this.block = block;
        this.energy = addDataSlot(energy);
        this.work = addDataSlot(work);
        addMachineSlot(inventory, MachineEntity.INPUT_SLOT, 53, changed);
        addMachineSlot(inventory, MachineEntity.OUTPUT_SLOT, 107, changed);
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, 83 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 141));
        }
    }

    private void addMachineSlot(ItemStackHandler inventory, int index, int x, Runnable changed) {
        addSlot(new SlotItemHandler(inventory, index, x, 40) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return storage ? ReBatteryItem.isBattery(stack) : index == MachineEntity.INPUT_SLOT && super.mayPlace(stack);
            }

            @Override
            public void setChanged() {
                changed.run();
            }
        });
    }

    private static DataSlot readOnly(IntSupplier value) {
        return new DataSlot() {
            @Override
            public int get() {
                return value.getAsInt();
            }

            @Override
            public void set(int ignored) {}
        };
    }

    int energy() {
        // Minecraft 1.21.1 sends menu data as signed shorts, but BatBox can hold 40,000 EU.
        return storage ? Short.toUnsignedInt((short) energy.get()) : energy.get();
    }

    int work() {
        return work.get();
    }

    boolean isStorage() {
        return storage;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, block);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        int input = storage && ReBatteryItem.isBattery(stack) && ReBatteryItem.charge(stack) > 0
            ? MachineEntity.OUTPUT_SLOT : MachineEntity.INPUT_SLOT;
        boolean moved = index < MachineEntity.SLOT_COUNT
            ? moveItemStackTo(stack, MachineEntity.SLOT_COUNT, slots.size(), true)
            : moveItemStackTo(stack, input, input + 1, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return before;
    }
}
