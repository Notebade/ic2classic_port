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

final class PowerMachineMenu extends AbstractContainerMenu implements ProcessingMenu {
    private boolean slotsVisible = true;
    private final RecipeCatalog catalog = new RecipeCatalog();
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
            if (kind == PowerMachines.Kind.WHISKY_BARREL) {
                x = slot < 3 ? 130 + slot * 28 : 144 + (slot - 3) * 28;
                y = slot < 3 ? 36 : 61;
            } else if (conversionPanel()) {
                x = 139;
                y = 52;
            } else if (plasmaPanel()) {
                x += kind == PowerMachines.Kind.FUSION ? 56 : 70;
                y += 24;
            } else if (steamPanel()) {
                x = 119 + slot % 2 * 28;
                y = 40 + slot / 2 * 22;
            } else if (generatorPanel()) {
                x = kind == PowerMachines.Kind.WINDMILL ? 133 : slot == 0 ? 148 : 112;
                y = kind == PowerMachines.Kind.WINDMILL || slot == 0 ? 58 : slot == 1 ? 42 : 74;
            }
            if (processing()) {
                x = switch (slot) { case 0 -> 228; case 1 -> 100; case 2 -> 176; case 3 -> 136; default -> 204; };
                y = slot == 0 ? 86 : 65;
            }
            addSlot(new SlotItemHandler(inventory, slot, x, y) {
                @Override public boolean isActive() { return slotsVisible; }
                @Override public boolean mayPlace(ItemStack stack) {
                    return !kind.outputSlot(index) && super.mayPlace(stack);
                }
            });
        }
        int inventoryY = conversionPanel() ? 109 : plasmaPanel() ? 132 : steamPanel() ? 124 : generatorPanel() ? 107
            : processing() ? AlloyGuiLayout.INVENTORY_SLOTS_Y : layout == null ? 132 : layout.inventoryY();
        int inventoryX = kind == PowerMachines.Kind.WHISKY_BARREL ? 42
            : plasmaPanel() ? 36 : processing() ? AlloyGuiLayout.SLOT_X : 8;
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(player, 9 + row * 9 + col, inventoryX + col * 18, inventoryY + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, inventoryX + col * 18, processing() ? AlloyGuiLayout.HOTBAR_Y : inventoryY + 58));
    }
    int value(int index) { return data.get(index * 2) & 65535 | data.get(index * 2 + 1) << 16; }
    boolean generatorPanel() {
        return kind == PowerMachines.Kind.WINDMILL || kind == PowerMachines.Kind.LIQUID_FUEL;
    }
    boolean steamPanel() {
        return kind == PowerMachines.Kind.BOILER || kind == PowerMachines.Kind.TUNNEL;
    }
    boolean plasmaPanel() {
        return kind == PowerMachines.Kind.FUSION || kind == PowerMachines.Kind.PLASMAFIER;
    }
    boolean conversionPanel() {
        return kind == PowerMachines.Kind.TRANSFORMER || kind.flux();
    }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (processing() && button >= 1000) return ProcessingTransfer.handle(this, this, player, button);
        if (machine == null || !stillValid(player) || button < 0 || button > 10 || !(kind == PowerMachines.Kind.TRANSFORMER || kind.chemical())) return false;
        machine.button(button); return true;
    }
    boolean processing() { return kind == PowerMachines.Kind.GASIFIER || kind == PowerMachines.Kind.ELECTRIC_GASIFIER || kind == PowerMachines.Kind.REFINERY; }
    @Override public MachineRecipeView.Process process() {
        return switch (kind) {
            case GASIFIER -> MachineRecipeView.Process.GASIFIER;
            case ELECTRIC_GASIFIER -> MachineRecipeView.Process.ELECTRIC_GASIFIER;
            case REFINERY -> MachineRecipeView.Process.REFINERY;
            default -> null;
        };
    }
    @Override public RecipeCatalog recipeCatalog() { return catalog; }
    @Override public int machineSlotCount() { return kind.slots; }
    @Override public java.util.List<Integer> recipeInputs(MachineRecipeView recipe) { return java.util.List.of(1); }
    @Override public void showMachineSlots(boolean visible) { slotsVisible = visible; }
    @Override public int storedEnergy() { return value(0); }
    @Override public int energyCapacity() { return kind.capacity; }
    @Override public int workProgress() { return value(13); }
    @Override public int workTotal() { return Math.max(1, value(17)); }
    @Override public java.util.List<net.minecraft.network.chat.Component> workDetails() {
        return kind == PowerMachines.Kind.REFINERY ? java.util.List.of(
            net.minecraft.network.chat.Component.translatable("gui.ic2.power_refinery_inputs", value(14), value(15)),
            net.minecraft.network.chat.Component.translatable("gui.ic2.power_refinery_output", value(16)))
            : java.util.List.of(net.minecraft.network.chat.Component.translatable("gui.ic2.power_gas_tank", value(14),
                kind == PowerMachines.Kind.GASIFIER ? 9000 : 18000));
    }
    @Override public java.util.List<ProcessingMenu.Control> controls() {
        return kind == PowerMachines.Kind.REFINERY ? java.util.List.of(
            new ProcessingMenu.Control(6, net.minecraft.network.chat.Component.literal("1 → out")),
            new ProcessingMenu.Control(7, net.minecraft.network.chat.Component.literal("2 → out")),
            new ProcessingMenu.Control(8, net.minecraft.network.chat.Component.literal("out → in"))) : java.util.List.of();
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
