package ic2.core.machine;

import ic2.core.IC2;
import java.util.List;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

final class SawmillRecipes {
    private final RecipeManager.CachedCheck<CraftingInput, CraftingRecipe> crafting =
        RecipeManager.createCheck(RecipeType.CRAFTING);

    ItemStack result(ItemStack stack, Level level) {
        if (stack.is(Items.MELON)) {
            return new ItemStack(Items.MELON_SLICE, 9);
        }
        if (stack.is(Items.POTATO)) {
            return new ItemStack(IC2.POTATO_SLICES.get(), 8);
        }
        if (level == null || !stack.is(ItemTags.LOGS)) {
            return ItemStack.EMPTY;
        }
        var input = CraftingInput.of(1, 1, List.of(stack.copyWithCount(1)));
        return crafting.getRecipeFor(input, level)
            .map(recipe -> recipe.value().assemble(input, level.registryAccess()))
            .filter(result -> result.is(ItemTags.PLANKS))
            .map(planks -> planks.copyWithCount(6))
            .orElse(ItemStack.EMPTY);
    }
}
