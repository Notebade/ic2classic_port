package ic2.core.machine;

import static ic2.core.machine.MachineRecipeView.Slot.fluid;
import static ic2.core.machine.MachineRecipeView.Slot.ingredient;
import static ic2.core.machine.MachineRecipeView.Slot.item;
import static ic2.core.machine.MachineRecipeView.Slot.items;

import ic2.core.IC2;
import ic2.core.item.FluidCellItem;
import ic2.core.machine.MachineRecipeView.Process;
import ic2.core.machine.MachineRecipeView.Slot;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.IntUnaryOperator;
import java.util.stream.Stream;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

final class MachineRecipeViews {
    private MachineRecipeViews() {}

    static List<MachineRecipeView> forProcess(Process process, Level level) {
        return switch (process) {
            case STONE_MACERATOR -> cooking(level, Machines.MACERATING.get(), ticks -> Math.max(1, ticks), 0);
            case MACERATOR -> cooking(level, Machines.MACERATING.get(), ticks -> Math.max(1, ticks), MachineKind.MACERATOR.energyPerTick);
            case IRON_FURNACE -> cooking(level, RecipeType.SMELTING, IronFurnaceBlockEntity::cookingTicks, 0);
            case ELECTRIC_FURNACE -> electricCooking(level, RecipeType.SMELTING, MachineKind.ELECTRIC_FURNACE);
            case ELECTRIC_BLAST_FURNACE -> electricCooking(level, RecipeType.BLASTING, MachineKind.ELECTRIC_BLAST_FURNACE);
            case ELECTRIC_SMOKER -> electricCooking(level, RecipeType.SMOKING, MachineKind.ELECTRIC_SMOKER);
            case EXTRACTOR -> registeredItems().map(input ->
                processing(input, ExtractionRecipes.result(input), MachineKind.EXTRACTOR)).flatMap(Stream::ofNullable).toList();
            case RARE_EARTH_EXTRACTOR -> registeredItems().map(MachineRecipeViews::rareEarth).flatMap(Stream::ofNullable).toList();
            case COMPRESSOR -> compressor();
            case ALLOY_SMELTER -> alloy();
            case SAWMILL -> sawmill(level);
            case RECYCLER -> recycling();
            case ELECTROLYZER -> registeredItems().flatMap(MachineRecipeViews::electrolysis).toList();
            case PUMP -> pump();
            case MASS_FABRICATOR -> massFabrication();
            case GENERATOR -> registeredItems().map(MachineRecipeViews::fuel).flatMap(Stream::ofNullable).toList();
            case GEOTHERMAL_GENERATOR -> fluidFuel(Fluids.LAVA, FluidGeneratorBlock.GEOTHERMAL_OUTPUT, PumpTank.BUCKET);
            case THERMAL_GENERATOR -> fluidFuel(Fluids.LAVA, note("thermal_fuel_output",
                ThermalProduction.ENERGY_PER_MB * PumpTank.BUCKET, ThermalProduction.MAX_OUTPUT), note("thermal_passive"));
            case WATER_MILL -> waterMill(MachineKind.WATER_MILL.waterMillTier);
            case LV_WATER_MILL -> waterMill(MachineKind.LV_WATER_MILL.waterMillTier);
            case MV_WATER_MILL -> waterMill(MachineKind.MV_WATER_MILL.waterMillTier);
            case HV_WATER_MILL -> waterMill(MachineKind.HV_WATER_MILL.waterMillTier);
        };
    }

    private static Stream<ItemStack> registeredItems() {
        return BuiltInRegistries.ITEM.stream().map(ItemStack::new).filter(stack -> !stack.isEmpty());
    }

    private static <T extends AbstractCookingRecipe> List<MachineRecipeView> electricCooking(
            Level level, RecipeType<T> type, MachineKind machine) {
        return cooking(level, type, machine::cookingTicks, machine.energyPerTick);
    }

    private static <T extends AbstractCookingRecipe> List<MachineRecipeView> cooking(
            Level level, RecipeType<T> type, IntUnaryOperator duration, int euPerTick) {
        return level.getRecipeManager().getAllRecipesFor(type).stream()
            .filter(holder -> !holder.value().getIngredients().isEmpty())
            .map(holder -> {
                T recipe = holder.value();
                int ticks = duration.applyAsInt(recipe.getCookingTime());
                return new MachineRecipeView(holder.id(), List.of(ingredient(recipe.getIngredients().getFirst(), 1)),
                    List.of(item(recipe.getResultItem(level.registryAccess()).copy())), List.of(cost(ticks, euPerTick)));
            }).toList();
    }

    private static MachineRecipeView processing(ItemStack input, ItemStack output, MachineKind kind) {
        return output.isEmpty() ? null : new MachineRecipeView(List.of(item(input)), List.of(item(output)),
            cost(kind.recipeTicks, kind.energyPerTick));
    }

    private static MachineRecipeView rareEarth(ItemStack input) {
        var recipe = RareEarthRecipes.find(input);
        if (recipe == null) {
            return null;
        }
        int count = Math.ceilDiv(RareEarthRecipes.UNITS_PER_OUTPUT, recipe.units());
        return new MachineRecipeView(List.of(item(input.copyWithCount(Math.min(count, input.getMaxStackSize())))),
            List.of(item(recipe.material().output())), note("batch", count),
            cost(count * MachineKind.RARE_EARTH_EXTRACTOR.recipeTicks, MachineKind.RARE_EARTH_EXTRACTOR.energyPerTick));
    }

    private static List<MachineRecipeView> compressor() {
        var recipes = new ArrayList<MachineRecipeView>();
        registeredItems().forEach(input -> {
            var recipe = CompressionRecipes.find(input);
            if (recipe != null) {
                recipes.add(processing(input.copyWithCount(recipe.inputCount()), new ItemStack(recipe.output()), MachineKind.COMPRESSOR));
            }
        });
        recipes.add(new MachineRecipeView(List.of(fluid(Fluids.WATER)), List.of(item(Items.SNOWBALL)),
            cost(MachineKind.COMPRESSOR.recipeTicks, MachineKind.COMPRESSOR.energyPerTick), note("adjacent_pump")));
        return recipes;
    }

    private static List<MachineRecipeView> alloy() {
        return AlloyRecipes.recipes().stream().map(recipe -> new MachineRecipeView(
            List.of(ingredient(recipe.first(), recipe.firstCount()), ingredient(recipe.second(), 1)),
            List.of(item(new ItemStack(recipe.output(), recipe.outputCount()))),
            cost(MachineKind.ALLOY_SMELTER.recipeTicks, MachineKind.ALLOY_SMELTER.energyPerTick))).toList();
    }

    private static List<MachineRecipeView> sawmill(Level level) {
        var sawmill = new SawmillRecipes();
        return registeredItems().map(input -> processing(input, sawmill.result(input, level), MachineKind.SAWMILL))
            .flatMap(Stream::ofNullable).toList();
    }

    private static List<MachineRecipeView> recycling() {
        Map<RecyclingRecipes.Recipe, List<ItemStack>> groups = new LinkedHashMap<>();
        registeredItems().forEach(input -> {
            var recipe = RecyclingRecipes.find(input);
            if (recipe != null) {
                groups.computeIfAbsent(recipe, unused -> new ArrayList<>()).add(input.copyWithCount(recipe.inputCount()));
            }
        });
        return groups.entrySet().stream().map(entry -> {
            var recipe = entry.getKey();
            return new MachineRecipeView(List.of(items(entry.getValue())), List.of(item(new ItemStack(recipe.output(), recipe.count()))),
                cost(MachineKind.RECYCLER.recipeTicks + recipe.extraTicks(), MachineKind.RECYCLER.energyPerTick),
                note("chance", recipe.chanceDenominator()));
        }).toList();
    }

    private static Stream<MachineRecipeView> electrolysis(ItemStack input) {
        var recipes = new ArrayList<MachineRecipeView>(2);
        var charge = Electrolysis.charge(input);
        if (charge != null) {
            recipes.add(new MachineRecipeView(List.of(item(input)), List.of(item(charge.output())),
                note("energy", charge.energy()), note("electrolysis_charge")));
        }
        var discharge = Electrolysis.discharge(input);
        if (discharge != null) {
            recipes.add(new MachineRecipeView(List.of(item(input)), List.of(item(discharge.input())),
                note("electrolysis_discharge"), note("electrolysis_output",
                    discharge.dischargeEnergy(1), discharge.dischargeEnergy(2), discharge.dischargeEnergy(3))));
        }
        return recipes.stream();
    }

    private static List<MachineRecipeView> pump() {
        return BuiltInRegistries.FLUID.stream()
            .filter(fluid -> fluid != Fluids.EMPTY && fluid.defaultFluidState().isSource() && fluid.getBucket() != Items.AIR)
            .flatMap(MachineRecipeViews::pump).toList();
    }

    private static Stream<MachineRecipeView> pump(Fluid liquid) {
        var source = fluid(liquid);
        var time = cost(MachineKind.PUMP.recipeTicks, MachineKind.PUMP.energyPerTick);
        var bucket = new MachineRecipeView(List.of(source, item(Items.BUCKET)), List.of(item(liquid.getBucket())),
            time, note("pump_source"));
        var tank = new MachineRecipeView(List.of(source), List.of(source), time, note("pump_tank"));
        ItemStack filled = FluidCellItem.filledStack(liquid);
        if (filled.isEmpty()) {
            return Stream.of(tank, bucket);
        }
        return Stream.of(tank, bucket, new MachineRecipeView(List.of(source, item(IC2.CELL_EMPTY.get())),
            List.of(item(filled)), time, note("pump_source")));
    }

    private static List<MachineRecipeView> massFabrication() {
        var output = List.of(item(IC2.UU_MATTER.get()));
        var energy = note("energy", MassFabrication.MATTER_COST);
        return Stream.concat(Stream.of(new MachineRecipeView(List.of(), output, energy)),
            registeredItems().filter(stack -> MassFabrication.amplifierValue(stack) > 0)
                .map(stack -> new MachineRecipeView(List.of(item(stack)), output, note("fabrication", MassFabrication.MATTER_COST),
                    note("amplifier", MassFabrication.amplifierValue(stack), MassFabrication.BONUS + 1)))).toList();
    }

    private static MachineRecipeView fuel(ItemStack stack) {
        int ticks = MachineEntity.fuelTicks(stack);
        if (ticks <= 0) {
            return null;
        }
        ItemStack remainder = stack.getCraftingRemainingItem();
        List<Slot> outputs = remainder.isEmpty() ? List.of() : List.of(item(remainder));
        return new MachineRecipeView(List.of(item(stack)), outputs,
            note("fuel_output", (long) ticks * MachineEntity.GENERATOR_OUTPUT, ticks));
    }

    private static List<MachineRecipeView> waterMill(WaterMillTier tier) {
        var recipes = new ArrayList<>(fluidFuel(Fluids.WATER, tier.output(), PumpTank.BUCKET / FluidGeneratorBlock.WATER_PER_TICK));
        String maxPassive = String.format(Locale.ROOT, "%.2f",
            tier.passiveUnits(tier.maxSources()) / (double) WaterMillTier.UNITS_PER_EU);
        recipes.add(new MachineRecipeView(List.of(fluid(Fluids.WATER)), List.of(), note("water_mill_passive", maxPassive)));
        return recipes;
    }

    private static List<MachineRecipeView> fluidFuel(Fluid fuel, int euPerTick, int ticks) {
        return fluidFuel(fuel, note("fuel_output", ticks * euPerTick, ticks));
    }

    private static List<MachineRecipeView> fluidFuel(Fluid fuel, Component... output) {
        return List.of(
            new MachineRecipeView(List.of(fluid(fuel)), List.of(), output),
            new MachineRecipeView(List.of(item(fuel.getBucket())), List.of(item(Items.BUCKET)), output),
            new MachineRecipeView(List.of(item(FluidCellItem.filledStack(fuel))), List.of(item(IC2.CELL_EMPTY.get())), output));
    }

    private static Component cost(int ticks, int euPerTick) {
        return euPerTick == 0 ? note("ticks", ticks) : note("cost", ticks, (long) ticks * euPerTick);
    }

    static Component note(String id, Object... arguments) {
        return Component.translatable("jei.ic2." + id, arguments);
    }
}
