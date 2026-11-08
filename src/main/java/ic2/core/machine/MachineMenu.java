package ic2.core.machine;

import ic2.core.item.ReBatteryItem;
import java.util.function.IntSupplier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

final class MachineMenu extends AbstractContainerMenu {
    private static final int ENERGY_LOW = 0;
    private static final int ENERGY_HIGH = 1;
    private static final int WORK_LOW = 2;
    private static final int WORK_HIGH = 3;
    private static final int FLUID = 4;
    private static final int DATA_COUNT = 5;
    private final ContainerLevelAccess access;
    private final Block block;
    private final ContainerData data;
    private final MachineKind storageKind;
    private final int machineSlots;

    MachineMenu(int id, Inventory playerInventory) {
        this(id, playerInventory, Machines.MENU.get());
    }

    static MachineMenu batbox(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.BATBOX_MENU.get());
    }

    static MachineMenu mfe(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.MFE_MENU.get());
    }

    static MachineMenu fabricator(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.FABRICATOR_MENU.get());
    }

    static MachineMenu geothermal(int id, Inventory playerInventory) {
        return new MachineMenu(id, playerInventory, Machines.GEOTHERMAL_MENU.get());
    }

    private MachineMenu(int id, Inventory playerInventory, MenuType<MachineMenu> type) {
        this(id, playerInventory, new ItemStackHandler(type == Machines.GEOTHERMAL_MENU.get()
                ? MachineEntity.SLOT_COUNT + 1 : MachineEntity.SLOT_COUNT),
            ContainerLevelAccess.NULL, Blocks.AIR, new SimpleContainerData(DATA_COUNT), () -> {}, type);
        data.set(FLUID, -1);
    }

    MachineMenu(int id, Inventory playerInventory, ItemStackHandler inventory, ContainerLevelAccess access,
                Block block, IntSupplier energy, IntSupplier work, IntSupplier fluid, Runnable changed,
                MenuType<MachineMenu> type) {
        this(id, playerInventory, inventory, access, block, readOnly(energy, work, fluid), changed, type);
    }

    private MachineMenu(int id, Inventory playerInventory, ItemStackHandler inventory, ContainerLevelAccess access,
                        Block block, ContainerData data, Runnable changed, MenuType<MachineMenu> type) {
        super(type, id);
        if (type == Machines.BATBOX_MENU.get()) {
            storageKind = MachineKind.BATBOX;
        } else if (type == Machines.MFE_MENU.get()) {
            storageKind = MachineKind.MFE;
        } else {
            storageKind = null;
        }
        this.access = access;
        this.block = block;
        this.data = data;
        machineSlots = inventory.getSlots();
        addDataSlots(data);
        if (isGeothermal()) {
            addMachineSlot(inventory, MachineEntity.INPUT_SLOT, 24, changed);
            addMachineSlot(inventory, MachineEntity.OUTPUT_SLOT, 80, changed);
            addMachineSlot(inventory, MachineEntity.GEOTHERMAL_BATTERY_SLOT, 136, changed);
        } else {
            addMachineSlot(inventory, MachineEntity.INPUT_SLOT, 53, changed);
            addMachineSlot(inventory, MachineEntity.OUTPUT_SLOT, 107, changed);
        }
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
                return isStorage() ? ReBatteryItem.isBattery(stack)
                    : index != MachineEntity.OUTPUT_SLOT && super.mayPlace(stack);
            }

            @Override
            public void setChanged() {
                changed.run();
            }
        });
    }

    private static ContainerData readOnly(IntSupplier energy, IntSupplier work, IntSupplier fluid) {
        return new ContainerData() {
            @Override
            public int get(int index) {
                // Minecraft 1.21.1 sends menu data as shorts.
                return switch (index) {
                    case ENERGY_LOW -> energy.getAsInt() & 0xffff;
                    case ENERGY_HIGH -> energy.getAsInt() >>> 16;
                    case WORK_LOW -> work.getAsInt() & 0xffff;
                    case WORK_HIGH -> work.getAsInt() >>> 16;
                    case FLUID -> fluid.getAsInt();
                    default -> throw new IndexOutOfBoundsException(index);
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    int energy() {
        return (data.get(ENERGY_HIGH) << 16) | (data.get(ENERGY_LOW) & 0xffff);
    }

    int work() {
        return (data.get(WORK_HIGH) << 16) | (data.get(WORK_LOW) & 0xffff);
    }

    int fluid() {
        return data.get(FLUID);
    }

    boolean isStorage() {
        return storageKind != null;
    }

    int storageCapacity() {
        return storageKind.capacity;
    }

    boolean isFabricator() {
        return getType() == Machines.FABRICATOR_MENU.get();
    }

    boolean isGeothermal() {
        return getType() == Machines.GEOTHERMAL_MENU.get();
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
        int input = MachineEntity.INPUT_SLOT;
        if (ReBatteryItem.isBattery(stack)) {
            if (isGeothermal()) {
                input = MachineEntity.GEOTHERMAL_BATTERY_SLOT;
            } else if (isStorage() && ReBatteryItem.charge(stack) > 0) {
                input = MachineEntity.OUTPUT_SLOT;
            }
        }
        boolean moved = index < machineSlots
            ? moveItemStackTo(stack, machineSlots, slots.size(), true)
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
