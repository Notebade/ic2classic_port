package ic2.core.machine;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;

final class ColossalRecipes {
    private ColossalRecipes() {}
    record Batch(ItemStack output, int inputCount, int chanceDenominator, float experience) {}

    static Batch find(ColossalMachines.Kind kind, ItemStack stack, Level level) {
        if (stack.isEmpty()) return null;
        return switch (kind) {
            case FURNACE, MACERATOR -> {
                var input = new SingleRecipeInput(stack);
                var type = kind == ColossalMachines.Kind.FURNACE ? RecipeType.SMELTING : Machines.MACERATING.get();
                yield level.getRecipeManager().getRecipeFor(type, input, level).map(holder -> new Batch(
                    holder.value().assemble(input, level.registryAccess()), 1, 1, holder.value().getExperience())).orElse(null);
            }
            case EXTRACTOR -> {
                var output = ExtractionRecipes.result(stack);
                yield output.isEmpty() ? null : new Batch(output, 1, 1, 0);
            }
            case COMPRESSOR -> {
                var recipe = CompressionRecipes.find(stack);
                yield recipe == null ? null : new Batch(new ItemStack(recipe.output()), recipe.inputCount(), 1, 0);
            }
            case RECYCLER -> {
                var recipe = RecyclingRecipes.find(stack);
                yield recipe == null ? null : new Batch(new ItemStack(recipe.output(), recipe.count()), recipe.inputCount(), recipe.chanceDenominator(), 0);
            }
        };
    }
}
