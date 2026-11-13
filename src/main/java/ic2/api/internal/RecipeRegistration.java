package ic2.api.internal;

import ic2.api.recipe.ProcessingRecipe;
import net.minecraft.resources.ResourceLocation;

/** Mod-owned lifecycle; not an addon extension point. */
public final class RecipeRegistration {
    public static final ExtensionRegistry<ResourceLocation, ProcessingRecipe> ENTRIES = new ExtensionRegistry<>();
    private RecipeRegistration() {}
    public static void close() { ENTRIES.freeze(); }
}
