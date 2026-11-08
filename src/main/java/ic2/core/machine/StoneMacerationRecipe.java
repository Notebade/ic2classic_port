package ic2.core.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;

final class StoneMacerationRecipe extends AbstractCookingRecipe {
    StoneMacerationRecipe(String group, CookingBookCategory category, Ingredient input,
            ItemStack output, float experience, int ticks) {
        super(Machines.STONE_MACERATING.get(), group, category, input, output, experience, ticks);
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return Machines.STONE_MACERATING_SERIALIZER.get();
    }

    @Override
    public boolean isSpecial() {
        return true;
    }
}
