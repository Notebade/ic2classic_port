package ic2.core.machine;

import ic2.api.machine.MachineUpgrade;
import ic2.api.machine.UpgradeContext;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class CobblegenMachineUpgradeItem extends Item implements MachineUpgrade {
    CobblegenMachineUpgradeItem(Properties properties) { super(properties); }
    @Override public Category category(ItemStack stack) { return Category.CUSTOM; }
    @Override public boolean tickUpgrade(ItemStack stack, UpgradeContext context) {
        if (context.level().getGameTime() % 20 == 0)
            net.neoforged.neoforge.items.ItemHandlerHelper.insertItemStacked(context.inputs(), new ItemStack(Items.COBBLESTONE, stack.getCount()), false);
        return false;
    }
}
