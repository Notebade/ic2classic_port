package ic2.core.machine;

import java.util.Comparator;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;

/** Stable client/server order, cached only for the lifetime of an open menu. */
final class RecipeCatalog {
    private MachineRecipeView.Process process;
    private List<MachineRecipeView> recipes = List.of();

    List<MachineRecipeView> get(MachineRecipeView.Process selected, Level level) {
        if (selected != process) {
            process = selected;
            recipes = selected == null ? List.of() : MachineRecipeViews.forProcess(selected, level).stream()
                .sorted(Comparator.comparing(RecipeCatalog::key)).toList();
        }
        return recipes;
    }

    private static String key(MachineRecipeView view) {
        if (view.sourceId() != null) return view.sourceId().toString();
        return view.inputs().stream().map(slot -> slot.items().stream()
            .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()) + "*" + stack.getCount())
            .sorted().toList().toString() + BuiltInRegistries.FLUID.getKey(slot.fluid().getFluid()) + "*" + slot.fluid().getAmount()).toList().toString()
            + view.outputs().stream().map(slot -> slot.items().stream()
                .map(stack -> BuiltInRegistries.ITEM.getKey(stack.getItem()) + "*" + stack.getCount())
                .sorted().toList().toString() + BuiltInRegistries.FLUID.getKey(slot.fluid().getFluid()) + "*" + slot.fluid().getAmount()).toList();
    }
}
