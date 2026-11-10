package ic2.core.machine;

import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

final class ReactorPlannerMenu extends AbstractContainerMenu {
    private static final int DATA_COUNT = 13;
    private final ReactorPlannerState planner;
    private final ContainerLevelAccess access;
    private final ContainerData data;
    private final DataSlot selection = DataSlot.standalone();
    private final List<ItemStack> electricParts;
    private final List<ItemStack> steamParts;

    ReactorPlannerMenu(int id, Inventory player) {
        this(id, player, new ItemStackHandler(ReactorRules.SLOT_COUNT), null, ContainerLevelAccess.NULL, new SimpleContainerData(DATA_COUNT));
    }

    ReactorPlannerMenu(int id, Inventory player, ReactorPlannerState planner, ContainerLevelAccess access) {
        this(id, player, planner.layout(), planner, access, new ContainerData() {
            @Override public int get(int index) { return planner.data(index); }
            @Override public void set(int index, int value) {}
            @Override public int getCount() { return DATA_COUNT; }
        });
    }

    private ReactorPlannerMenu(int id, Inventory player, ItemStackHandler layout, ReactorPlannerState planner,
                               ContainerLevelAccess access, ContainerData data) {
        super(Machines.REACTOR_PLANNER_MENU.get(), id);
        this.planner = planner;
        this.access = access;
        this.data = data;
        electricParts = components(false);
        steamParts = components(true);
        addDataSlots(data);
        addDataSlot(selection);
        for (int slot = 0; slot < ReactorRules.SLOT_COUNT; slot++) {
            addSlot(new SlotItemHandler(layout, slot, 8 + slot % ReactorRules.COLUMNS * 18, 56 + slot / ReactorRules.COLUMNS * 18) {
                @Override public boolean isActive() { return getSlotIndex() % ReactorRules.COLUMNS < width(); }
                @Override public boolean mayPickup(Player player) { return false; }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
            });
        }
        for (int index = 0; index < 36; index++) {
            addSlot(new Slot(player, index, 8 + index % 9 * 18, index < 9 ? 254 : 196 + (index / 9 - 1) * 18));
        }
    }

    int width() { return Math.max(ReactorRules.BASE_COLUMNS, data.get(0)); }
    boolean steam() { return data.get(1) != 0; }
    boolean running() { return data.get(2) != 0; }
    int heat() { return readInt(3); }
    int maxHeat() { return readInt(5); }
    int output() { return readInt(7); }
    int cycles() { return readInt(9); }
    int startingHeat() { return readInt(11); }

    ItemStack selectedPart() {
        List<ItemStack> parts = steam() ? steamParts : electricParts;
        return parts.get(Math.floorMod(selection.get(), parts.size()));
    }

    private static List<ItemStack> components(boolean steam) {
        return BuiltInRegistries.ITEM.stream().map(item -> item.getDefaultInstance())
            .filter(stack -> ReactorGrid.accepts(stack, steam)).toList();
    }

    private int readInt(int index) {
        return (data.get(index) & 0xffff) | (data.get(index + 1) << 16);
    }

    @Override
    public void clicked(int slot, int button, ClickType type, Player player) {
        if (slot >= 0 && slot < ReactorRules.SLOT_COUNT) {
            if (planner != null && type == ClickType.PICKUP && stillValid(player)) {
                ItemStack carried = getCarried();
                ItemStack component = carried.isEmpty() ? selectedPart() : carried;
                planner.setComponent(slot, button == 0 ? component : ItemStack.EMPTY);
            }
            return;
        }
        super.clicked(slot, button, type, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int command) {
        if (planner == null || !stillValid(player)) {
            return false;
        }
        if (command == 6 || command == 7) {
            int count = (steam() ? steamParts : electricParts).size();
            selection.set(Math.floorMod(selection.get() + (command == 6 ? -1 : 1), count));
            return true;
        }
        return planner.command(command);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(access, player, Machines.REACTOR_PLANNER.get());
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }
}
