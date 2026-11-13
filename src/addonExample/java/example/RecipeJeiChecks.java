package example;

import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.runtime.IJeiRuntime;
import net.minecraft.resources.ResourceLocation;

@JeiPlugin
public final class RecipeJeiChecks implements IModPlugin {
    private static IJeiRuntime runtime;
    @Override public ResourceLocation getPluginUid() { return ResourceLocation.fromNamespaceAndPath("exampleaddon", "recipe_checks"); }
    @Override public void onRuntimeAvailable(IJeiRuntime available) { runtime = available; }

    static void showEnrichment() {
        if (runtime == null) throw new AssertionError("JEI runtime missing");
        var type = runtime.getRecipeManager().getRecipeType(ResourceLocation.parse("ic2:uranium_enricher")).orElseThrow();
        if (runtime.getRecipeManager().createRecipeLookup(type).get().count() < 5) throw new AssertionError("JEI enrichment recipes missing");
        runtime.getRecipesGui().showTypes(List.of(type));
    }
}
