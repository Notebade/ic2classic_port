package ic2.api.item;

import net.minecraft.world.item.ItemStack;

/** Optional per-stack admission to native toolboxes; the ic2:toolbox_tools tag also admits items. */
public interface BoxableItem {
    boolean canStoreInToolbox(ItemStack stack);
}
