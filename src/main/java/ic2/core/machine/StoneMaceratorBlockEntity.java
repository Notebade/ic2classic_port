package ic2.core.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

final class StoneMaceratorBlockEntity extends AbstractFurnaceBlockEntity {
    StoneMaceratorBlockEntity(BlockPos pos, BlockState state) {
        super(Machines.STONE_MACERATOR_ENTITY.get(), pos, state, Machines.MACERATING.get());
    }

    @Override
    protected int getBurnDuration(ItemStack fuel) {
        return fuel.getBurnTime(RecipeType.SMELTING) / 2;
    }

    @Override
    protected Component getDefaultName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        return new StoneMaceratorMenu(id, inventory, this, dataAccess);
    }
}
