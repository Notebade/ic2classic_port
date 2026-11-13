package ic2.core.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;

public final class ScrapFuelItem extends Item {
    public ScrapFuelItem(Properties properties) { super(properties); }
    @Override public int getBurnTime(ItemStack stack, RecipeType<?> type) { return 350; }
}
