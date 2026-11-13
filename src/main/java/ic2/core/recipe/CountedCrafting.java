package ic2.core.recipe;

import net.minecraft.world.item.ItemStack;

/** Custom crafting recipes whose individual grid slots consume more than one item. */
public interface CountedCrafting {
    int consumed(ItemStack stack);
}
