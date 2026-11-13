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
        new Recipe(Ingredient.of(Items.NETHERITE_SCRAP), 2, Ingredient.of(Items.GOLD_INGOT), 2, Items.NETHERITE_INGOT, 1),
        new Recipe(Ingredient.of(Items.COPPER_BLOCK), 1, Ingredient.of(IC2.TIN_INGOT.get()), 3, IC2.BRONZE_INGOT.get(), 12),
        new Recipe(Ingredient.of(Items.REDSTONE), 2, Ingredient.of(Items.QUARTZ), IC2.PULSATING_QUARTZ.get(), 1),
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
                return recipe.batch(false);
            }
            if (recipe.matches(second, first)) {
                return recipe.batch(true);
            }
        }
        return null;
    }

    record Recipe(Ingredient first, int firstCount, Ingredient second, int secondCount, Item output, int outputCount) {
        Recipe(Ingredient first, int firstCount, Ingredient second, Item output, int outputCount) {
            this(first, firstCount, second, 1, output, outputCount);
        }

        boolean matches(ItemStack firstStack, ItemStack secondStack) {
            return new AlloyInputAmounts(firstCount, secondCount).fits(firstStack.getCount(), secondStack.getCount())
                && first.test(firstStack) && second.test(secondStack);
        }

        Batch batch(boolean reversed) {
            var amounts = new AlloyInputAmounts(firstCount, secondCount);
            if (reversed) amounts = amounts.reversed();
            return new Batch(amounts.first(), amounts.second(), new ItemStack(output, outputCount));
        }
    }

    record Batch(int firstCount, int secondCount, ItemStack output) {}
}
