package ic2.core.machine;

import ic2.core.item.RechargeableItem;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;

final class ElectricWrenchItem extends RechargeableItem {
    private final int energyPerUse;

    ElectricWrenchItem(Properties properties) {
        this(properties, false);
    }

    ElectricWrenchItem(Properties properties, boolean precision) {
        super(properties, precision ? 40_000 : 12_000, precision ? 350 : 250, precision ? 128 : 32);
        energyPerUse = precision ? 100 : 50;
    }

    @Override
    public boolean canProvideEnergy() {
        return false;
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return WrenchItem.useOnMachine(context, cost -> ic2.api.item.ElectricEquipment.canUse(stack, cost * energyPerUse),
            cost -> ic2.api.item.ElectricEquipment.use(stack, cost * energyPerUse, context.getPlayer()));
    }
}
