package ic2.core.machine;

import ic2.core.IC2;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;

final class RefineryRecipes {
    record Recipe(String id, Fluid first, int firstAmount, Fluid second, int secondAmount, Item item, int count,
                  Item output, int outputCount, Fluid fluidOutput, int fluidAmount, int ticks) {}
    record Plan(Recipe recipe, int firstTank, int secondTank, ItemStack itemOutput, FluidStack fluidOutput) {}
    private static List<Recipe> recipes;
    static List<Recipe> recipes() {
        if (recipes == null) recipes = List.of(
            new Recipe("polished_gold", alcohol(), 500, Fluids.EMPTY, 0, Items.GOLD_INGOT, 1, PowerMachines.POLISHED_GOLD.get(), 1, Fluids.EMPTY, 0, 250),
            new Recipe("bio_gas_fuel", GeneratorFluids.Fuel.WOODGAS.fluid.get(), 1000, Fluids.WATER, 1000, Items.AIR, 0, Items.AIR, 0, GeneratorFluids.Fuel.BIO.fluid.get(), 100, 500),
            new Recipe("bio_fuel", Fluids.WATER, 2000, Fluids.EMPTY, 0, BoilerFuel.COMPRESSED_PLANT.get(), 2, Items.AIR, 0, GeneratorFluids.Fuel.BIO.fluid.get(), 500, 250),
            new Recipe("unrefined_whisky", Fluids.WATER, 1000, Fluids.EMPTY, 0, ChemistryMaterials.FLOUR.get(), 4, Items.AIR, 0, ChemistryMaterials.WHISKY.get(), 250, 125),
            new Recipe("refined_whisky", ChemistryMaterials.WHISKY.get(), 1000, Fluids.EMPTY, 0, Items.AIR, 0, ChemistryMaterials.GRIST.get(), 1, Fluids.EMPTY, 0, 250),
            new Recipe("whisky_mug", Fluids.WATER, 4000, Fluids.EMPTY, 0, ChemistryMaterials.WHISKY_MUG.get(), 1, ChemistryMaterials.MUG.get(), 1, alcohol(), 10000, 250),
            new Recipe("coal_dust_washing", Fluids.WATER, 100, Fluids.EMPTY, 0,
                BuiltInRegistries.ITEM.get(ResourceLocation.parse("ic2:dust_coal")), 1, BoilerFuel.HYDRATED_DUST.get(), 1, Fluids.EMPTY, 0, 25));
        return recipes;
    }
    private static Fluid alcohol() { return GeneratorFluids.Fuel.ALCOHOL.fluid.get(); }
    static boolean validFluid(Fluid fluid) {
        return fluid == Fluids.WATER || fluid == ChemistryMaterials.WHISKY.get() || GeneratorFluids.fuel(fluid) != null;
    }
    static boolean accepts(ItemStack stack) { return recipes().stream().anyMatch(recipe -> recipe.count > 0 && stack.is(recipe.item)); }
    static Plan find(ItemStack input, FluidStack first, FluidStack second) {
        for (Recipe recipe : recipes()) {
            if (recipe.count == 0 ? !input.isEmpty() : !input.is(recipe.item) || input.getCount() < recipe.count) continue;
            int firstIndex = matches(first, recipe.first, recipe.firstAmount) && matches(second, recipe.second, recipe.secondAmount) ? 0
                : matches(second, recipe.first, recipe.firstAmount) && matches(first, recipe.second, recipe.secondAmount) ? 1 : -1;
            if (firstIndex < 0) continue;
            int fluidAmount = recipe.fluidAmount;
            if (input.is(ChemistryMaterials.WHISKY_MUG.get())) {
                fluidAmount = WhiskyCycle.alcohol(ChemistryMaterials.grade(input)); if (fluidAmount == 0) continue;
            }
            return new Plan(recipe, firstIndex, 1 - firstIndex,
                recipe.outputCount == 0 ? ItemStack.EMPTY : new ItemStack(recipe.output, recipe.outputCount),
                fluidAmount == 0 ? FluidStack.EMPTY : new FluidStack(recipe.fluidOutput, fluidAmount));
        }
        return null;
    }
    private static boolean matches(FluidStack stack, Fluid fluid, int amount) { return amount == 0 || stack.is(fluid) && stack.getAmount() >= amount; }
}
