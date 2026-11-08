package ic2.core.machine;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractFurnaceMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.RecipeBookType;

final class StoneMaceratorMenu extends AbstractFurnaceMenu {
    StoneMaceratorMenu(int id, Inventory inventory) {
        super(Machines.STONE_MACERATOR_MENU.get(), Machines.MACERATING.get(), RecipeBookType.FURNACE, id, inventory);
    }

    StoneMaceratorMenu(int id, Inventory inventory, Container machine, ContainerData data) {
        super(Machines.STONE_MACERATOR_MENU.get(), Machines.MACERATING.get(), RecipeBookType.FURNACE,
            id, inventory, machine, data);
    }
}
