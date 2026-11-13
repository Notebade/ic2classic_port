package ic2.core.machine;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.crafting.RecipeType;

final class IronFurnaceMenu extends FuelProcessingMenu {
    IronFurnaceMenu(int id, Inventory player) { this(id, player, new SimpleContainer(3), new SimpleContainerData(4)); }
    IronFurnaceMenu(int id, Inventory player, Container inventory, ContainerData data) {
        super(Machines.IRON_FURNACE_MENU.get(), RecipeType.SMELTING, id, player, inventory, data, MachineRecipeView.Process.IRON_FURNACE);
    }
}
