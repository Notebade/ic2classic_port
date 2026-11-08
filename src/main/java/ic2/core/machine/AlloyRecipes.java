package ic2.core.machine;

import ic2.core.IC2;
import java.util.List;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;

final class AlloyRecipes {
    private static final List<Recipe> RECIPES = List.of(
        new Recipe(Ingredient.of(Items.COPPER_INGOT), 3, Ingredient.of(IC2.TIN_INGOT.get()), IC2.BRONZE_INGOT.get(), 4),
        new Recipe(Ingredient.of(IC2.COPPER_DUST.get()), 3, Ingredient.of(IC2.TIN_DUST.get()), IC2.BRONZE_INGOT.get(), 4),
        new Recipe(Ingredient.of(IC2.ALUMINIUM_DUST.get()), 1, Ingredient.of(ItemTags.SAND), IC2.ALUMINIUM_INGOT.get(), 1)
    );

    private AlloyRecipes() {}

    static List<Recipe> recipes() {
        return RECIPES;
    }

    static boolean accepts(ItemStack stack, ItemStack other) {
        return RECIPES.stream().anyMatch(recipe ->
            recipe.first().test(stack) && (other.isEmpty() || recipe.second().test(other))
                || recipe.second().test(stack) && (other.isEmpty() || recipe.first().test(other)));
    }

    static Batch find(ItemStack first, ItemStack second) {
        for (Recipe recipe : RECIPES) {
            if (recipe.matches(first, second)) {
                return recipe.batch(recipe.firstCount(), 1);
            }
            if (recipe.matches(second, first)) {
                return recipe.batch(1, recipe.firstCount());
            }
        }
        return null;
    }

    record Recipe(Ingredient first, int firstCount, Ingredient second, Item output, int outputCount) {
        boolean matches(ItemStack firstStack, ItemStack secondStack) {
            return firstStack.getCount() >= firstCount && first.test(firstStack) && second.test(secondStack);
        }

        Batch batch(int firstAmount, int secondAmount) {
            return new Batch(firstAmount, secondAmount, new ItemStack(output, outputCount));
        }
    }

    record Batch(int firstCount, int secondCount, ItemStack output) {}
}
