package ic2.core.recipe;

import ic2.core.IC2;
import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraft.world.level.Level;

public final class ReactorFuelCellRecipe extends CustomRecipe {
    public static final RecipeSerializer<ReactorFuelCellRecipe> SERIALIZER =
        new SimpleCraftingRecipeSerializer<>(ReactorFuelCellRecipe::new);
    private static final int DUST_COUNT = 8;

    private ReactorFuelCellRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (input.ingredientCount() != 3) {
            return false;
        }
        boolean cell = false;
        boolean clay = false;
        boolean dust = false;
        for (ItemStack stack : input.items()) {
            cell |= stack.is(IC2.CELL_EMPTY.get());
            clay |= stack.is(IC2.CLAY_DUST.get());
            dust |= isCountedIngredient(stack) && stack.getCount() >= DUST_COUNT;
        }
        return cell && clay && dust;
    }

    public static boolean isCountedIngredient(ItemStack stack) {
        return stack.is(IC2.NETHERRACK_DUST.get());
    }

    public static int consumption(ItemStack stack) {
        return isCountedIngredient(stack) ? DUST_COUNT : 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        return getResultItem(registries);
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return new ItemStack(IC2.REACTOR_FUEL_CELL.get());
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 3;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return IC2.REACTOR_FUEL_CELL_RECIPE.get();
    }
}
