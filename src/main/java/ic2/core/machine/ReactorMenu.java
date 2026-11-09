package ic2.core.machine;

import java.util.function.IntSupplier;
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

final class ReactorMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 9;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    ReactorMenu(int id, Inventory player) {
        this(id, player, false);
    }

    static ReactorMenu steam(int id, Inventory player) {
        return new ReactorMenu(id, player, true);
    }

    private ReactorMenu(int id, Inventory player, boolean steam) {
        this(id, player, new ItemStackHandler(ReactorRules.SLOT_COUNT), ContainerLevelAccess.NULL, new SimpleContainerData(DATA_COUNT), steam);
    }

    ReactorMenu(int id, Inventory player, ItemStackHandler inventory, ContainerLevelAccess access,
                IntSupplier width, IntSupplier heat, IntSupplier output, IntSupplier maxHeat, SteamReactorTanks tanks) {
        this(id, player, inventory, access, new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> width.getAsInt();
                    case 1, 2 -> dataWord(heat.getAsInt(), index - 1);
                    case 3, 4 -> dataWord(output.getAsInt(), index - 3);
                    case 5, 6 -> dataWord(maxHeat.getAsInt(), index - 5);
                    case 7, 8 -> tanks == null ? 0 : tanks.getFluidInTank(index - 7).getAmount();
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {}

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        }, tanks != null);
    }

    private ReactorMenu(int id, Inventory player, ItemStackHandler inventory, ContainerLevelAccess access, ContainerData data, boolean steam) {
        super(steam ? Machines.STEAM_REACTOR_MENU.get() : Machines.NUCLEAR_REACTOR_MENU.get(), id);
        this.access = access;
        this.data = data;
        addDataSlots(data);
        for (int row = 0; row < ReactorRules.ROWS; row++) {
            for (int column = 0; column < ReactorRules.COLUMNS; column++) {
                int slot = row * ReactorRules.COLUMNS + column;
                addSlot(new SlotItemHandler(inventory, slot, 8 + column * 18, 39 + row * 18) {
                    @Override
                    public boolean isActive() {
                        return getSlotIndex() % ReactorRules.COLUMNS < width();
                    }

                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return isActive() && ReactorGrid.accepts(stack, steam) && super.mayPlace(stack);
                    }
                });
            }
        }
        int inventoryOffset = steam ? 12 : 0;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(player, 9 + row * 9 + column, 8 + column * 18, 156 + inventoryOffset + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(player, column, 8 + column * 18, 214 + inventoryOffset));
        }
    }

    int width() {
        return Math.max(ReactorRules.BASE_COLUMNS, data.get(0));
    }

    int heat() {
        return dataInt(1);
    }

    int output() {
        return dataInt(3);
    }

    int maxHeat() {
        return Math.max(ReactorRules.MAX_HEAT, dataInt(5));
    }

    boolean isSteam() {
        return getType() == Machines.STEAM_REACTOR_MENU.get();
    }

    int water() {
        return data.get(7);
    }

    int steam() {
        return data.get(8);
    }

    // Vanilla menu packets send signed 16-bit values.
    private static int dataWord(int value, int word) {
        return (short) (value >>> (16 * word));
    }

    private int dataInt(int index) {
        return (data.get(index) & 0xffff) | (data.get(index + 1) << 16);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, isSteam() ? Machines.STEAM_REACTOR.get() : Machines.NUCLEAR_REACTOR.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        boolean moved = index < ReactorRules.SLOT_COUNT
            ? moveItemStackTo(stack, ReactorRules.SLOT_COUNT, slots.size(), true)
            : moveItemStackTo(stack, 0, ReactorRules.SLOT_COUNT, false);
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
