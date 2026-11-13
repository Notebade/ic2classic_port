package ic2.core.machine;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;

/** Keeps vanilla fuel/result slot behavior and experience awards. */
class FuelProcessingMenu extends AbstractFurnaceMenu implements ProcessingMenu {
    private boolean visible = true;
    private final MachineRecipeView.Process process;
    private final RecipeCatalog catalog = new RecipeCatalog();

    FuelProcessingMenu(MenuType<?> type, RecipeType<? extends AbstractCookingRecipe> recipes, int id,
                       Inventory player, Container inventory, ContainerData data, MachineRecipeView.Process process) {
        super(type, recipes, RecipeBookType.FURNACE, id, player, inventory, data);
        this.process = process;
        for (int i = 0; i < slots.size(); i++) {
            Slot old = slots.get(i);
            int x = i < 3 ? (i == 2 ? 204 : i == 1 ? 228 : 100) : AlloyGuiLayout.SLOT_X + (i - 3) % 9 * 18;
            int y = i < 3 ? (i == 1 ? 86 : 65) : i < 30 ? AlloyGuiLayout.INVENTORY_SLOTS_Y + (i - 3) / 9 * 18 : AlloyGuiLayout.HOTBAR_Y;
            Slot next = i == 0 ? new Slot(inventory, 0, x, y) { @Override public boolean isActive() { return visible; } }
                : i == 1 ? new FurnaceFuelSlot(this, inventory, 1, x, y) { @Override public boolean isActive() { return visible; } }
                : i == 2 ? new FurnaceResultSlot(player.player, inventory, 2, x, y) { @Override public boolean isActive() { return visible; } }
                : new Slot(old.container, old.getContainerSlot(), x, y);
            next.index = i;
            slots.set(i, next);
        }
    }

    @Override public boolean clickMenuButton(Player player, int button) { return ProcessingTransfer.handle(this, this, player, button); }
    @Override public MachineRecipeView.Process process() { return process; }
    @Override public RecipeCatalog recipeCatalog() { return catalog; }
    @Override public int machineSlotCount() { return 3; }
    @Override public List<Integer> recipeInputs(MachineRecipeView recipe) { return List.of(0); }
    @Override public void showMachineSlots(boolean show) { visible = show; }
    @Override public int storedEnergy() { return 0; }
    @Override public int energyCapacity() { return 0; }
    @Override public int gaugeValue() { return (int) (getLitProgress() * 1000); }
    @Override public int gaugeCapacity() { return 1000; }
    @Override public int workProgress() { return (int) (getBurnProgress() * 1000); }
    @Override public int workTotal() { return 1000; }
    @Override public List<Component> workDetails() { return List.of(Component.translatable(isLit() ? "gui.ic2.remastered.running" : "gui.ic2.remastered.fuel_wait")); }
}
