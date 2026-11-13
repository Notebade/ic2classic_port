package ic2.core.machine;

import ic2.core.IC2;
import java.util.List;
import java.util.Map;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

final class ExtractionRecipes {
    private static final Map<Item, Recipe> ITEMS = Map.ofEntries(
        Map.entry(BoilerFuel.HYDRATED_CELL.get(), new Recipe(BoilerFuel.COAL_CELL.get(), 1)),
        Map.entry(BoilerFuel.RAW_BIO_CELL.get(), new Recipe(BoilerFuel.BIO_CELL.get(), 1)),
        Map.entry(IC2.STICKY_RESIN.get(), new Recipe(IC2.RUBBER.get(), 3)),
        Map.entry(Items.GRAVEL, new Recipe(Items.FLINT, 2)),
        Map.entry(Items.POPPY, new Recipe(Items.RED_DYE, 3)),
        Map.entry(Items.DANDELION, new Recipe(Items.YELLOW_DYE, 3)),
        Map.entry(Items.BLUE_ORCHID, new Recipe(Items.LIGHT_BLUE_DYE, 3)),
        Map.entry(Items.ALLIUM, new Recipe(Items.MAGENTA_DYE, 3)),
        Map.entry(Items.AZURE_BLUET, new Recipe(Items.LIGHT_GRAY_DYE, 3)),
        Map.entry(Items.RED_TULIP, new Recipe(Items.RED_DYE, 3)),
        Map.entry(Items.ORANGE_TULIP, new Recipe(Items.ORANGE_DYE, 3)),
        Map.entry(Items.WHITE_TULIP, new Recipe(Items.LIGHT_GRAY_DYE, 3)),
        Map.entry(Items.PINK_TULIP, new Recipe(Items.PINK_DYE, 3)),
        Map.entry(Items.OXEYE_DAISY, new Recipe(Items.LIGHT_GRAY_DYE, 3)),
        Map.entry(Items.CORNFLOWER, new Recipe(Items.BLUE_DYE, 3)),
        Map.entry(Items.LILY_OF_THE_VALLEY, new Recipe(Items.WHITE_DYE, 3)),
        Map.entry(Items.WITHER_ROSE, new Recipe(Items.BLACK_DYE, 3)),
        Map.entry(Items.BEETROOT, new Recipe(Items.RED_DYE, 3)),
        Map.entry(Items.SUNFLOWER, new Recipe(Items.YELLOW_DYE, 6)),
        Map.entry(Items.LILAC, new Recipe(Items.MAGENTA_DYE, 6)),
        Map.entry(Items.ROSE_BUSH, new Recipe(Items.RED_DYE, 6)),
        Map.entry(Items.PEONY, new Recipe(Items.PINK_DYE, 6)),
        Map.entry(Items.MAGMA_CREAM, new Recipe(Items.SLIME_BALL, 1))
    );

    private static final List<TaggedRecipe> ORES = List.of(
        new TaggedRecipe(Tags.Items.ORES_COAL, new Recipe(Items.COAL, 5)),
        new TaggedRecipe(Tags.Items.ORES_LAPIS, new Recipe(Items.LAPIS_LAZULI, 10)),
        new TaggedRecipe(Tags.Items.ORES_DIAMOND, new Recipe(Items.DIAMOND, 3)),
        new TaggedRecipe(Tags.Items.ORES_EMERALD, new Recipe(Items.EMERALD, 3)),
        new TaggedRecipe(Tags.Items.ORES_QUARTZ, new Recipe(Items.QUARTZ, 3))
    );

    private ExtractionRecipes() {}

    static Recipe find(ItemStack stack) {
        Recipe builtin = builtin(stack);
        if (builtin != null) return builtin;
        return ic2.api.recipe.Ic2Recipes.additions(ic2.api.recipe.ProcessingRecipe.Process.EXTRACTION).stream()
            .filter(recipe -> stack.is(recipe.input())).findFirst().map(recipe -> new Recipe(recipe.output(), recipe.outputCount())).orElse(null);
    }

    static Recipe builtin(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Recipe recipe = ITEMS.get(stack.getItem());
        if (recipe != null) {
            return recipe;
        }
        for (TaggedRecipe ore : ORES) {
            if (stack.is(ore.input())) {
                return ore.recipe();
            }
        }
        return null;
    }

    static ItemStack result(ItemStack stack) {
        Recipe recipe = find(stack);
        return recipe == null ? ItemStack.EMPTY : new ItemStack(recipe.output(), recipe.count());
    }

    record Recipe(Item output, int count) {}

    private record TaggedRecipe(TagKey<Item> input, Recipe recipe) {}
}
