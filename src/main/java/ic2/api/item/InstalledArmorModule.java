package ic2.api.item;

import java.util.Objects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Detached inventory view; editing the returned stack cannot edit the armor. */
public record InstalledArmorModule(ResourceLocation category, ItemStack stack) {
    public InstalledArmorModule { Objects.requireNonNull(category); stack = stack.copy(); }
    @Override public ItemStack stack() { return stack.copy(); }
}
