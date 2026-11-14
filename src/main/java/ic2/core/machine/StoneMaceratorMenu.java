package ic2.core.machine;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeBookType;

final class StoneMaceratorMenu extends FuelProcessingMenu {
    StoneMaceratorMenu(int id, Inventory inventory) {
        this(id, inventory, new net.minecraft.world.SimpleContainer(3), new net.minecraft.world.inventory.SimpleContainerData(4));
    }

    StoneMaceratorMenu(int id, Inventory inventory, Container machine, ContainerData data) {
        super(Machines.STONE_MACERATOR_MENU.get(), Machines.MACERATING.get(), id, inventory, machine, data,
            MachineRecipeView.Process.STONE_MACERATOR);
    }
}
