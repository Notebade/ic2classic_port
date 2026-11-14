package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.FurnaceMenu;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class IronFurnaceBlockEntity extends AbstractFurnaceBlockEntity {
    public static int cookingTicks(int vanillaTicks) {
        return Math.max(1, (int) (vanillaTicks * 4L / 5));
    }

    IronFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(Machines.IRON_FURNACE_ENTITY.get(), pos, state, RecipeType.SMELTING);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("block.ic2.iron_furnace");
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new IronFurnaceMenu(id, inventory, this, dataAccess);
    }
}
