package ic2.api.recipe;

import ic2.api.internal.ExtensionRegistry;
import ic2.api.internal.RecipeRegistration;
import ic2.core.machine.AddonRecipeBridge;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Register during FMLCommonSetupEvent.enqueueWork; immutable after mod loading completes. */
public final class Ic2Recipes {
    private static final ExtensionRegistry<ResourceLocation, ProcessingRecipe> ADDITIONS = RecipeRegistration.ENTRIES;
    private Ic2Recipes() {}
    public static void addCompression(ResourceLocation id, Item input, int inputCount, Item output) {
        register(new ProcessingRecipe(id, ProcessingRecipe.Process.COMPRESSION, input, inputCount, output, 1));
    }
    public static void addExtraction(ResourceLocation id, Item input, Item output, int outputCount) {
        register(new ProcessingRecipe(id, ProcessingRecipe.Process.EXTRACTION, input, 1, output, outputCount));
    }
    private static synchronized void register(ProcessingRecipe recipe) {
        if (recipe.id().getNamespace().equals("ic2") || recipe.id().getNamespace().equals("minecraft"))
            throw new IllegalArgumentException("Use your addon namespace for recipe IDs");
        if (ADDITIONS.values().stream().anyMatch(existing -> existing.process() == recipe.process() && existing.input() == recipe.input()))
            throw new IllegalArgumentException("Addon input already registered for " + recipe.process());
        ADDITIONS.register(recipe.id(), recipe);
    }
    /** Addon entries only. Builtin recipes keep priority over additions. */
    public static List<ProcessingRecipe> additions(ProcessingRecipe.Process process) {
        Objects.requireNonNull(process, "process");
        return ADDITIONS.values().stream().filter(recipe -> recipe.process() == process).toList();
    }
    /** Effective recipe lookup, including builtin tag matches. Does not require inputCount items. */
    public static Optional<ProcessingRecipe> find(ProcessingRecipe.Process process, ItemStack input) {
        return AddonRecipeBridge.find(Objects.requireNonNull(process, "process"), Objects.requireNonNull(input, "input"));
    }
    /** Snapshot of effective recipes for all registered items; query after world tags are loaded. */
    public static List<ProcessingRecipe> recipes(ProcessingRecipe.Process process) {
        Objects.requireNonNull(process, "process");
        return BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(stack -> !stack.isEmpty())
            .flatMap(stack -> find(process, stack).stream()).toList();
    }
}
