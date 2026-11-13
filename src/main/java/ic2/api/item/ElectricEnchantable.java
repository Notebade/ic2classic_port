package ic2.api.item;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;

/** Optional addon Item contract for the electric enchanter, including otherwise non-enchantable tools. */
public interface ElectricEnchantable {
    boolean acceptsElectricEnchantment(ItemStack stack, Holder<Enchantment> enchantment, boolean anvil);
    default int electricEnchantability(ItemStack stack) { return 1; }
}
