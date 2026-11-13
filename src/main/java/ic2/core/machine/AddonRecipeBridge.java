package ic2.core.machine;

import ic2.api.recipe.Ic2Recipes;
import ic2.api.recipe.ProcessingRecipe;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Internal adapter. Addons should use ic2.api.recipe.Ic2Recipes. */
public final class AddonRecipeBridge {
    private AddonRecipeBridge() {}
    public static Optional<ProcessingRecipe> find(ProcessingRecipe.Process process, ItemStack input) {
        if (input.isEmpty()) return Optional.empty();
        Item output; int consumed, produced;
        if (process == ProcessingRecipe.Process.COMPRESSION) {
            var recipe = CompressionRecipes.find(input);
            if (recipe == null) return Optional.empty();
            output = recipe.output(); consumed = recipe.inputCount(); produced = 1;
        } else {
            var recipe = ExtractionRecipes.find(input);
            if (recipe == null) return Optional.empty();
            output = recipe.output(); consumed = 1; produced = recipe.count();
        }
        boolean builtin = process == ProcessingRecipe.Process.COMPRESSION ? CompressionRecipes.builtin(input) != null : ExtractionRecipes.builtin(input) != null;
        Optional<ProcessingRecipe> addition = builtin ? Optional.empty() : Ic2Recipes.additions(process).stream().filter(recipe -> recipe.input() == input.getItem()
            && recipe.output() == output && recipe.inputCount() == consumed && recipe.outputCount() == produced).findFirst();
        var inputId = BuiltInRegistries.ITEM.getKey(input.getItem());
        return addition.or(() -> Optional.of(new ProcessingRecipe(ResourceLocation.fromNamespaceAndPath("ic2", "builtin/"
            + process.name().toLowerCase(java.util.Locale.ROOT) + "/" + inputId.getNamespace() + "/" + inputId.getPath()),
            process, input.getItem(), consumed, output, produced)));
    }
}
