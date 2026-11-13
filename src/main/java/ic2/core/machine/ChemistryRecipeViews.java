package ic2.core.machine;

import java.util.ArrayList;
import java.util.List;
import ic2.core.machine.MachineRecipeView.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

final class ChemistryRecipeViews {
    private ChemistryRecipeViews() {}
    static List<MachineRecipeView> refinery() {
        var views = new ArrayList<MachineRecipeView>();
        for (var recipe : RefineryRecipes.recipes()) {
            if (recipe.item() == ChemistryMaterials.WHISKY_MUG.get()) {
                for (int grade = 1; grade <= 5; grade++) views.add(view(recipe, ChemistryMaterials.whiskyMug(grade), WhiskyCycle.alcohol(grade)));
            } else views.add(view(recipe, recipe.count() == 0 ? ItemStack.EMPTY : new ItemStack(recipe.item(), recipe.count()), recipe.fluidAmount()));
        }
        return List.copyOf(views);
    }
    private static MachineRecipeView view(RefineryRecipes.Recipe recipe, ItemStack catalyst, int fluidAmount) {
        var inputs = new ArrayList<Slot>(); var outputs = new ArrayList<Slot>();
        if (recipe.firstAmount() > 0) inputs.add(new Slot(List.of(), new FluidStack(recipe.first(), recipe.firstAmount())));
        if (recipe.secondAmount() > 0) inputs.add(new Slot(List.of(), new FluidStack(recipe.second(), recipe.secondAmount())));
        if (!catalyst.isEmpty()) inputs.add(Slot.item(catalyst));
        if (recipe.outputCount() > 0) outputs.add(Slot.item(new ItemStack(recipe.output(), recipe.outputCount())));
        if (fluidAmount > 0) outputs.add(new Slot(List.of(), new FluidStack(recipe.fluidOutput(), fluidAmount)));
        return new MachineRecipeView(inputs, outputs, MachineRecipeViews.note("chemical_recipe", recipe.ticks(), recipe.ticks() * 20));
    }
}
