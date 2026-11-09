package ic2.core.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class ReactorReflectorItem extends Item {
    private final boolean repairable;

    public ReactorReflectorItem(Properties properties, int durability, boolean repairable) {
        super(properties.durability(durability).setNoRepair());
        this.repairable = repairable;
    }

    public boolean reflects(ItemStack stack) {
        return !repairable || stack.getDamageValue() < stack.getMaxDamage();
    }

    public boolean repairable() {
        return repairable;
    }
}
