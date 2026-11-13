package ic2.core.machine;

import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.Level;

/** Capabilities shared by implemented processing menus, never by unrelated utility menus. */
interface ProcessingMenu {
    MachineRecipeView.Process process();
    int machineSlotCount();
    List<Integer> recipeInputs(MachineRecipeView recipe);
    default List<Integer> bulkInputs(MachineRecipeView recipe) { return recipeInputs(recipe); }
    default boolean acceptsTransfer(MachineRecipeView recipe, List<net.minecraft.world.item.ItemStack> inputs, Level level) {
        var menu = (net.minecraft.world.inventory.AbstractContainerMenu) this;
        var targets = recipeInputs(recipe);
        for (int i = 0; i < targets.size(); i++) if (!menu.getSlot(targets.get(i)).mayPlace(inputs.get(i))) return false;
        return true;
    }
    void showMachineSlots(boolean visible);
    int storedEnergy();
    int energyCapacity();
    default int gaugeValue() { return storedEnergy(); }
    default int gaugeCapacity() { return energyCapacity(); }
    int workProgress();
    int workTotal();
    List<Component> workDetails();
    default Component controlName() { return Component.empty(); }
    default int controlId() { return 0; }
    record Control(int id, Component label) {}
    default List<Control> controls() { return controlName().getString().isEmpty() ? List.of() : List.of(new Control(controlId(), controlName())); }
    RecipeCatalog recipeCatalog();
    default List<MachineRecipeView> recipeViews(Level level) { return recipeCatalog().get(process(), level); }
}
