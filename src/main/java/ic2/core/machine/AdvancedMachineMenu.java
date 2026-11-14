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

final class AdvancedMachineMenu extends AbstractContainerMenu implements ProcessingMenu {
    private boolean slotsVisible = true;
    private final RecipeCatalog recipeCatalog = new RecipeCatalog();
    private final ContainerData data;
    private final ContainerLevelAccess access;
    private final AdvancedMachineEntity machine;
    final AdvancedMachines.Variant variant;

    AdvancedMachineMenu(int id, Inventory player) {
        this(id, player, AdvancedMachines.Variant.FURNACE);
    }

    AdvancedMachineMenu(int id, Inventory player, AdvancedMachines.Variant variant) {
        this(id, player, new ItemStackHandler(variant.layout.slots() + 2), new SimpleContainerData(15), ContainerLevelAccess.NULL, null, variant);
        data.set(13, variant.ordinal());
    }

    AdvancedMachineMenu(int id, Inventory player, ItemStackHandler inventory, ContainerData data,
                  ContainerLevelAccess access, AdvancedMachineEntity machine, AdvancedMachines.Variant variant) {
        super(variant.menu(), id);
        this.variant = variant;
        this.data = data;
        this.access = access;
        this.machine = machine;
        addDataSlots(data);
        boolean queued = variant == AdvancedMachines.Variant.SLOW;
        addSlot(inputSlot(inventory, 0, 228, 86));
        for (int input = 1; input <= variant.layout.inputs(); input++) {
            int x = queued ? 90 + (input - 1) % 3 * 18 : 100 + (input - 1) * 20;
            int y = queued ? 50 + (input - 1) / 3 * 18 : 65;
            addSlot(inputSlot(inventory, input, x, y));
        }
        int firstOutput = variant.layout.inputs() + 1;
        for (int index = firstOutput; index < variant.layout.slots(); index++) {
            addSlot(new SlotItemHandler(inventory, index, 176 + (index - firstOutput) * 18, 65) {
                @Override public boolean isActive() { return slotsVisible; }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public void onTake(Player owner, ItemStack stack) {
                    if (machine != null) machine.awardExperience(owner.position());
                    stack.onCraftedBy(owner.level(), owner, stack.getCount());
                    super.onTake(owner, stack);
                }
            });
        }
        addSlot(inputSlot(inventory, variant.layout.slots(), 146, 108));
        addSlot(inputSlot(inventory, variant.layout.slots() + 1, 164, 108));
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) addSlot(new Slot(player, 9 + row * 9 + col, AlloyGuiLayout.SLOT_X + col * 18, AlloyGuiLayout.INVENTORY_SLOTS_Y + row * 18));
        }
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, AlloyGuiLayout.SLOT_X + col * 18, AlloyGuiLayout.HOTBAR_Y));
    }

    int energy() { return data.get(0) & 65535 | data.get(7) << 16; }
    int capacity() { return data.get(8) & 65535 | data.get(9) << 16; }
    int packet() { return data.get(10) & 65535 | data.get(11) << 16; }
    int heat() { return data.get(1) & 65535 | data.get(14) << 16; }
    int progress() { return data.get(2); }
    boolean split() { return data.get(3) != 0; }
    int materialProgress() { return data.get(5); }
    int grinderMode() { return Math.max(1, data.get(6)); }

    @Override public boolean clickMenuButton(Player player, int id) {
        if (id >= 1000) return ProcessingTransfer.handle(this, this, player, id);
        if (id != 0 || !(variant.layout.supportsSplit() || variant == AdvancedMachines.Variant.SLOW) || !stillValid(player)) return false;
        if (machine != null) {
            if (variant == AdvancedMachines.Variant.SLOW) machine.cycleGrinderMode();
            else machine.toggleSplit();
        }
        return true;
    }

    private Slot inputSlot(ItemStackHandler inventory, int index, int x, int y) {
        return new SlotItemHandler(inventory, index, x, y) {
            @Override public boolean isActive() { return slotsVisible; }
        };
    }
    private AdvancedMachines.Variant displayedVariant() {
        return AdvancedMachines.Variant.values()[Math.clamp(data.get(13), 0, AdvancedMachines.Variant.values().length - 1)];
    }
    @Override public MachineRecipeView.Process process() {
        return switch (displayedVariant()) {
            case FURNACE -> MachineRecipeView.Process.INDUCTION_FURNACE;
            case BLAST -> MachineRecipeView.Process.INDUCTION_BLAST_FURNACE;
            case SMOKER -> MachineRecipeView.Process.INDUCTION_SMOKER;
            case ROTARY -> MachineRecipeView.Process.ROTARY_MACERATOR;
            case CENTRIFUGAL -> MachineRecipeView.Process.CENTRIFUGAL_EXTRACTOR;
            case SINGULARITY -> MachineRecipeView.Process.SINGULARITY_COMPRESSOR;
            case RARE_EARTH -> MachineRecipeView.Process.CENTRIFUGAL_RARE_EARTH_EXTRACTOR;
            case COMPACTING -> MachineRecipeView.Process.COMPACTING_RECYCLER;
            case SLOW -> MachineRecipeView.Process.SLOW_GRINDER;
            case PRESSURE -> MachineRecipeView.Process.PRESSURE_ALLOY_FURNACE;
        };
    }
    @Override public RecipeCatalog recipeCatalog() { return recipeCatalog; }
    @Override public int machineSlotCount() { return variant.layout.slots() + 2; }
    @Override public java.util.List<Integer> recipeInputs(MachineRecipeView recipe) {
        return recipe.inputs().size() == 2 && variant == AdvancedMachines.Variant.PRESSURE ? java.util.List.of(1, 2) : java.util.List.of(1);
    }
    @Override public void showMachineSlots(boolean visible) { slotsVisible = visible; }
    @Override public boolean acceptsTransfer(MachineRecipeView recipe, java.util.List<ItemStack> inputs, net.minecraft.world.level.Level level) {
        return variant == AdvancedMachines.Variant.PRESSURE ? inputs.size() == 2 && AlloyRecipes.find(inputs.get(0), inputs.get(1)) != null
            : ProcessingMenu.super.acceptsTransfer(recipe, inputs, level);
    }
    @Override public java.util.List<Integer> bulkInputs(MachineRecipeView recipe) {
        return recipe.inputs().size() == 1 ? java.util.stream.IntStream.rangeClosed(1, variant.layout.inputs()).boxed().toList() : recipeInputs(recipe);
    }
    @Override public int storedEnergy() { return energy(); }
    @Override public int energyCapacity() { return capacity(); }
    @Override public int workProgress() { return progress(); }
    @Override public int workTotal() { return 1000; }
    @Override public java.util.List<net.minecraft.network.chat.Component> workDetails() {
        String metric = switch (displayedVariant()) {
            case ROTARY, CENTRIFUGAL, RARE_EARTH, COMPACTING -> "gui.ic2.advanced_speed";
            case SLOW -> "gui.ic2.grinder_chance";
            case SINGULARITY -> "gui.ic2.advanced_pressure";
            default -> "gui.ic2.induction_heat";
        };
        return java.util.List.of(net.minecraft.network.chat.Component.translatable(metric,
            variant == AdvancedMachines.Variant.SLOW ? RecyclerControl.chancePercent(grinderMode(), 4) : heat() / 100));
    }
    @Override public net.minecraft.network.chat.Component controlName() {
        if (variant == AdvancedMachines.Variant.SLOW) return net.minecraft.network.chat.Component.translatable("gui.ic2.grinder_mode", grinderMode());
        return variant.layout.supportsSplit() ? net.minecraft.network.chat.Component.translatable(split() ? "gui.ic2.induction_split" : "gui.ic2.induction_shared")
            : net.minecraft.network.chat.Component.empty();
    }

    @Override public boolean stillValid(Player player) {
        if (machine != null && !machine.isOperational()) return false;
        return access.evaluate((level, pos) -> level.getBlockState(pos).getBlock() instanceof AdvancedMachineBlock
            && player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) <= 64, true);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack before = stack.copy();
        int machineSlots = variant.layout.slots() + 2;
        boolean moved = index < machineSlots ? moveItemStackTo(stack, machineSlots, slots.size(), true)
            : moveItemStackTo(stack, variant.layout.slots(), machineSlots, false)
                || moveItemStackTo(stack, 0, 1, false) || moveItemStackTo(stack, 1, variant.layout.inputs() + 1, false);
        if (!moved) return ItemStack.EMPTY;
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        slot.onTake(player, before.copyWithCount(before.getCount() - stack.getCount()));
        return before;
    }
}
