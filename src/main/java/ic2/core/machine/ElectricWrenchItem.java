package ic2.core.machine;

import ic2.core.item.RechargeableItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

final class ElectricWrenchItem extends RechargeableItem {
    private static final int ENERGY_PER_USE = 50;

    ElectricWrenchItem(Properties properties) {
        super(properties, 12_000, 250, 32);
    }

    @Override
    public boolean canProvideEnergy() {
        return false;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        int stored = charge(stack);
        return WrenchItem.useOnMachine(context, cost -> stored >= cost * ENERGY_PER_USE,
            cost -> setCharge(stack, stored - cost * ENERGY_PER_USE));
    }
}
