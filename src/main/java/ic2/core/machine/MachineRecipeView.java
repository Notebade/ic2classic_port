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
        ROTARY_MACERATOR(AdvancedMachines.ROTARY),
        INDUCTION_FURNACE(AdvancedMachines.FURNACE),
        INDUCTION_BLAST_FURNACE(AdvancedMachines.BLAST),
        INDUCTION_SMOKER(AdvancedMachines.SMOKER),
        CENTRIFUGAL_EXTRACTOR(AdvancedMachines.CENTRIFUGAL),
        SINGULARITY_COMPRESSOR(AdvancedMachines.SINGULARITY),
        IRON_FURNACE(Machines.IRON_FURNACE),
        ELECTRIC_FURNACE(Machines.ELECTRIC_FURNACE),
        ELECTRIC_BLAST_FURNACE(Machines.ELECTRIC_BLAST_FURNACE),
        ELECTRIC_SMOKER(Machines.ELECTRIC_SMOKER),
        EXTRACTOR(Machines.EXTRACTOR),
        RARE_EARTH_EXTRACTOR(Machines.RARE_EARTH_EXTRACTOR),
        COMPRESSOR(Machines.COMPRESSOR),
        ALLOY_SMELTER(Machines.ALLOY_SMELTER),
        CANNER(Machines.CANNER),
        VACUUM_CANNER(Machines.VACUUM_CANNER),
        URANIUM_ENRICHER(Machines.URANIUM_ENRICHER),
        COLOSSAL_FURNACE(ColossalMachines.MACHINES.get(ColossalMachines.Kind.FURNACE)),
        COLOSSAL_MACERATOR(ColossalMachines.MACHINES.get(ColossalMachines.Kind.MACERATOR)),
        COLOSSAL_EXTRACTOR(ColossalMachines.MACHINES.get(ColossalMachines.Kind.EXTRACTOR)),
        COLOSSAL_COMPRESSOR(ColossalMachines.MACHINES.get(ColossalMachines.Kind.COMPRESSOR)),
        COLOSSAL_RECYCLER(ColossalMachines.MACHINES.get(ColossalMachines.Kind.RECYCLER)),
        PRESSURE_ALLOY_FURNACE(AdvancedMachines.PRESSURE),
        SAWMILL(Machines.SAWMILL),
        RECYCLER(Machines.RECYCLER),
        COMPACTING_RECYCLER(AdvancedMachines.COMPACTING),
        SLOW_GRINDER(AdvancedMachines.SLOW),
        ELECTROLYZER(Machines.ELECTROLYZER),
        CHARGED_ELECTROLYZER(Machines.CHARGED_ELECTROLYZER),
        CENTRIFUGAL_RARE_EARTH_EXTRACTOR(AdvancedMachines.RARE_EARTH),
        PUMP(Machines.PUMP),
        MASS_FABRICATOR(Machines.MASS_FABRICATOR),
        GENERATOR(Machines.GENERATOR),
        SOLAR_TURBINE(ExtraGenerators.SOLAR),
        SLAG_GENERATOR(ExtraGenerators.SLAG),
        WINDMILL(PowerMachines.MACHINES.get(PowerMachines.Kind.WINDMILL)),
        LIQUID_FUEL_GENERATOR(PowerMachines.MACHINES.get(PowerMachines.Kind.LIQUID_FUEL)),
        GASIFIER(PowerMachines.MACHINES.get(PowerMachines.Kind.GASIFIER)),
        ELECTRIC_GASIFIER(PowerMachines.MACHINES.get(PowerMachines.Kind.ELECTRIC_GASIFIER)),
        REFINERY(PowerMachines.MACHINES.get(PowerMachines.Kind.REFINERY)),
        WATER_MILL(Machines.WATER_MILL),
        LV_WATER_MILL(Machines.LV_WATER_MILL),
        MV_WATER_MILL(Machines.MV_WATER_MILL),
        HV_WATER_MILL(Machines.HV_WATER_MILL),
        GEOTHERMAL_GENERATOR(Machines.GEOTHERMAL),
        THERMAL_GENERATOR(Machines.THERMAL_GENERATOR);

        final DeferredBlock<?> machine;

        Process(DeferredBlock<?> machine) {
            this.machine = machine;
        }
    }
}
