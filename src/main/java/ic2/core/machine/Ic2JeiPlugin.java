package ic2.core.machine;

import ic2.core.IC2;
import ic2.core.machine.MachineRecipeView.Process;
import ic2.core.recipe.UuMatterRecipe;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.RecipeTypes;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.ingredient.ICraftingGridHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.extensions.vanilla.crafting.ICraftingCategoryExtension;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.IVanillaCategoryExtensionRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

@JeiPlugin
public final class Ic2JeiPlugin implements IModPlugin {
    @Override
    public ResourceLocation getPluginUid() {
        return ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var gui = registration.getJeiHelpers().getGuiHelper();
        for (Process process : Process.values()) {
            registration.addRecipeCategories(new MachineRecipeCategory(process, gui));
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (Process process : Process.values()) {
            registration.addRecipeCatalyst(process.machine.get(), MachineRecipeCategory.type(process));
        }
        registration.addRecipeCatalyst(Machines.IRON_FURNACE.get(), RecipeTypes.FUELING);
        registration.addRecipeCatalyst(AdvancedMachines.FURNACE.get(), RecipeTypes.SMELTING);
        registration.addRecipeCatalyst(AdvancedMachines.BLAST.get(), RecipeTypes.BLASTING);
        registration.addRecipeCatalyst(AdvancedMachines.SMOKER.get(), RecipeTypes.SMOKING);
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        var level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        for (Process process : Process.values()) {
            registration.addRecipes(MachineRecipeCategory.type(process), MachineRecipeViews.forProcess(process, level));
        }
        registration.addIngredientInfo(IC2.STICKY_RESIN.get(), MachineRecipeViews.note("resin"));
        registration.addIngredientInfo(IC2.SCRAP_BOX.get(), MachineRecipeViews.note("scrapbox"));
        registration.addIngredientInfo(IC2.CELL_EMPTY.get(), MachineRecipeViews.note("fluid_cells"));
        registration.addIngredientInfo(Machines.STONE_MACERATOR.get(), MachineRecipeViews.note("stone_fuel"));
        registration.addIngredientInfo(Machines.WIND_TURBINE.get(), MachineRecipeViews.note("wind_turbine"));
        registration.addIngredientInfo(Machines.WAVE_GENERATOR.get(), MachineRecipeViews.note("wave_generator"));
        registration.addIngredientInfo(Machines.OCEAN_GENERATOR.get(), MachineRecipeViews.note("ocean_generator"));
        registration.addIngredientInfo(Machines.NUCLEAR_REACTOR.get(), MachineRecipeViews.note("nuclear_reactor"));
        registration.addIngredientInfo(Machines.CHARGED_ELECTROLYZER.get(), MachineRecipeViews.note("charged_electrolyzer"));
        registration.addIngredientInfo(AdvancedMachines.PRESSURE.get(), MachineRecipeViews.note("pressure_build"));
    }

    @Override
    public void registerVanillaCategoryExtensions(IVanillaCategoryExtensionRegistration registration) {
        registration.getCraftingCategory().addExtension(UuMatterRecipe.class, new UuCrafting());
    }

    private static final class UuCrafting implements ICraftingCategoryExtension<UuMatterRecipe> {
        @Override
        public void setRecipe(RecipeHolder<UuMatterRecipe> holder, IRecipeLayoutBuilder builder, ICraftingGridHelper grid,
                              IFocusGroup focuses) {
            var recipe = holder.value();
            List<List<ItemStack>> inputs = recipe.pattern().stream().flatMap(row -> row.chars().mapToObj(symbol ->
                symbol == 'M' ? List.of(new ItemStack(IC2.UU_MATTER.get())) : List.<ItemStack>of())).toList();
            grid.createAndSetInputs(builder, inputs, 3, 3);
            grid.createAndSetOutputs(builder, List.of(recipe.result().copy()));
        }

        @Override
        public int getWidth(RecipeHolder<UuMatterRecipe> recipe) {
            return 3;
        }

        @Override
        public int getHeight(RecipeHolder<UuMatterRecipe> recipe) {
            return 3;
        }
    }
}
