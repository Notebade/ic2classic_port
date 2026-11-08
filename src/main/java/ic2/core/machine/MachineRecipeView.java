package ic2.core.machine;

import java.util.Arrays;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.registries.DeferredBlock;

record MachineRecipeView(ResourceLocation sourceId, List<Slot> inputs, List<Slot> outputs, List<Component> notes) {
    MachineRecipeView(List<Slot> inputs, List<Slot> outputs, Component... notes) {
        this(null, inputs, outputs, List.of(notes));
    }

    record Slot(List<ItemStack> items, FluidStack fluid) {
        static Slot item(ItemLike item) {
            return item(new ItemStack(item));
        }

        static Slot item(ItemStack item) {
            return items(List.of(item));
        }

        static Slot items(List<ItemStack> items) {
            return new Slot(items, FluidStack.EMPTY);
        }

        static Slot ingredient(Ingredient ingredient, int count) {
            return items(Arrays.stream(ingredient.getItems()).map(stack -> stack.copyWithCount(count)).toList());
        }

        static Slot fluid(Fluid fluid) {
            return new Slot(List.of(), new FluidStack(fluid, PumpTank.BUCKET));
        }
    }

    enum Process {
        STONE_MACERATOR(Machines.STONE_MACERATOR),
        MACERATOR(Machines.MACERATOR),
        IRON_FURNACE(Machines.IRON_FURNACE),
        ELECTRIC_FURNACE(Machines.ELECTRIC_FURNACE),
        ELECTRIC_BLAST_FURNACE(Machines.ELECTRIC_BLAST_FURNACE),
        ELECTRIC_SMOKER(Machines.ELECTRIC_SMOKER),
        EXTRACTOR(Machines.EXTRACTOR),
        RARE_EARTH_EXTRACTOR(Machines.RARE_EARTH_EXTRACTOR),
        COMPRESSOR(Machines.COMPRESSOR),
        ALLOY_SMELTER(Machines.ALLOY_SMELTER),
        SAWMILL(Machines.SAWMILL),
        RECYCLER(Machines.RECYCLER),
        ELECTROLYZER(Machines.ELECTROLYZER),
        PUMP(Machines.PUMP),
        MASS_FABRICATOR(Machines.MASS_FABRICATOR),
        GENERATOR(Machines.GENERATOR),
        GEOTHERMAL_GENERATOR(Machines.GEOTHERMAL);

        final DeferredBlock<?> machine;

        Process(DeferredBlock<?> machine) {
            this.machine = machine;
        }
    }
}
