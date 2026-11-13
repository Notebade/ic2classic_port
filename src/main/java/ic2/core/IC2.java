package ic2.core;

import com.mojang.serialization.Codec;
import ic2.core.item.FluidCellItem;
import ic2.core.item.RechargeableItem;
import ic2.core.item.PortableBatteryItem;
import ic2.core.item.ReactorReflectorItem;
import ic2.core.item.ScrapBoxItem;
import ic2.core.item.UraniumFuelRod;
import ic2.core.item.ReactorPulseCell;
import ic2.core.machine.Machines;
import ic2.core.machine.AdvancedMachines;
import ic2.core.machine.ExtraGenerators;
import ic2.core.machine.PowerMachines;
import ic2.core.recipe.PowerCraftingRecipe;
import ic2.core.item.ScrapFuelItem;
import ic2.core.recipe.ReactorComponentRepair;
import ic2.core.recipe.ReactorFuelCellRecipe;
import ic2.core.recipe.UuMatterRecipe;
import ic2.core.tree.Rubberwood;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(IC2.MOD_ID)
public final class IC2 {
    public static final String MOD_ID = "ic2";
    private static final int LAPOTRON_CAPACITY = 1_000_000;
    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, MOD_ID);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PowerCraftingRecipe>> PLASMA_CORE_RECIPE =
        RECIPE_SERIALIZERS.register("plasma_core", () -> PowerCraftingRecipe.serializer(PowerCraftingRecipe.Kind.CORE));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PowerCraftingRecipe>> BOILER_CASING_RECIPE =
        RECIPE_SERIALIZERS.register("fuel_boiler_structure", () -> PowerCraftingRecipe.serializer(PowerCraftingRecipe.Kind.BOILER));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PowerCraftingRecipe>> FUEL_CAN_RECIPE =
        RECIPE_SERIALIZERS.register("fuel_can_fill", () -> PowerCraftingRecipe.serializer(PowerCraftingRecipe.Kind.FUEL));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<UuMatterRecipe>> UU_MATTER_RECIPE =
        RECIPE_SERIALIZERS.register("uu_matter", () -> UuMatterRecipe.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ReactorComponentRepair>> REACTOR_COMPONENT_REPAIR =
        RECIPE_SERIALIZERS.register("reactor_component_repair", () -> ReactorComponentRepair.SERIALIZER);
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ReactorFuelCellRecipe>> REACTOR_FUEL_CELL_RECIPE =
        RECIPE_SERIALIZERS.register("reactor_fuel_cell", () -> ReactorFuelCellRecipe.SERIALIZER);
    private static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BATTERY_CHARGE =
        COMPONENTS.registerComponentType("battery_charge", builder -> builder
            .persistent(Codec.intRange(0, 50_000_000)));
    public static final DeferredItem<RechargeableItem> RE_BATTERY = rechargeable("re_battery", 10_000, 100, 32);
    public static final DeferredItem<RechargeableItem> ENERGY_CRYSTAL = rechargeable("energy_crystal", 100_000, 250, 128);
    public static final DeferredItem<RechargeableItem> LAPOTRON_CRYSTAL = rechargeable("lapatron_crystal", LAPOTRON_CAPACITY, 600, 512);
    public static final DeferredItem<RechargeableItem> GLOWTRONIC_CRYSTAL = rechargeable("glowtronic_crystal", 7_500_000, 2500, 512);
    public static final DeferredItem<RechargeableItem> UESC = rechargeable("uesc", 17_500_000, 9500, 2048);
    public static final DeferredItem<RechargeableItem> PESD = rechargeable("pesd", 50_000_000, 25_000, 2048);
    public static final DeferredItem<RechargeableItem> QUANTUM_ACCUMULATOR = rechargeable("quantum_accumulator", 900_000, 1000, 32);
    public static final DeferredItem<RechargeableItem> BIG_QUANTUM_ACCUMULATOR = rechargeable("quantum_accumulator_big", 5_400_000, 6000, 32);
    public static final DeferredItem<Item> PULSATING_QUARTZ = ITEMS.registerSimpleItem("pulsating_quartz");
    public static final DeferredItem<Item> COMPLEX_CIRCUIT = ITEMS.registerSimpleItem("complex_circuit");

    private static DeferredItem<RechargeableItem> rechargeable(String name, int capacity, int transfer, int packet) {
        return ITEMS.registerItem(name, properties -> new PortableBatteryItem(properties, capacity, transfer, packet),
            new Item.Properties().stacksTo(1));
    }
    public static final DeferredItem<Item> COPPER_DUST = ITEMS.registerSimpleItem("dust_copper");
    public static final DeferredItem<Item> RUBBER = ITEMS.registerSimpleItem("rubber");
    public static final DeferredItem<Item> STICKY_RESIN = ITEMS.registerSimpleItem("sticky_resin");
    public static final DeferredItem<Item> DENSE_COPPER_PLATE = ITEMS.registerSimpleItem("plate_dense_copper");
    public static final DeferredItem<Item> URANIUM_INGOT = ITEMS.registerSimpleItem("ingot_uranium");
    public static final DeferredItem<UraniumFuelRod> URANIUM_ROD =
        ITEMS.registerItem("uranium_rod_single", properties -> new UraniumFuelRod(properties, 1));
    public static final DeferredItem<UraniumFuelRod> DUAL_URANIUM_ROD =
        ITEMS.registerItem("uranium_rod_dual", properties -> new UraniumFuelRod(properties, 2));
    public static final DeferredItem<UraniumFuelRod> QUAD_URANIUM_ROD =
        ITEMS.registerItem("uranium_rod_quad", properties -> new UraniumFuelRod(properties, 4));
    public static final DeferredItem<Item> DEPLETED_URANIUM_ROD = ITEMS.registerSimpleItem("uranium_rod_near_depleted");
    public static final DeferredItem<Item> RE_ENRICHED_URANIUM_ROD = ITEMS.registerSimpleItem("uranium_rod_re_enriched");
    public static final DeferredItem<ReactorPulseCell> ISOTOPIC_URANIUM_ROD =
        ITEMS.registerItem("uranium_rod_isotopic", properties -> new ReactorPulseCell(properties, 10_000, RE_ENRICHED_URANIUM_ROD));
    public static final DeferredItem<Item> CELL_FUEL = ITEMS.registerSimpleItem("cell_fuel");
    public static final DeferredItem<ReactorPulseCell> REACTOR_FUEL_CELL = ITEMS.registerItem("reactor_fuel_cell",
        properties -> new ReactorPulseCell(properties, 2_500, CELL_FUEL, 1_000));
    public static final DeferredItem<Item> PREPARED_FUEL_CELL = ITEMS.registerSimpleItem("prepared_fuel_cell");
    public static final DeferredItem<Item> CLAY_DUST = ITEMS.registerSimpleItem("dust_clay");
    public static final DeferredItem<Item> NETHERRACK_DUST = ITEMS.registerSimpleItem("dust_netherrack");
    public static final DeferredItem<Item> HEAT_PACK = ITEMS.registerSimpleItem("heat_pack");
    public static final DeferredItem<ReactorReflectorItem> NEUTRON_REFLECTOR = ITEMS.registerItem("reflector",
        properties -> new ReactorReflectorItem(properties, 10_000, false));
    public static final DeferredItem<ReactorReflectorItem> THICK_NEUTRON_REFLECTOR = ITEMS.registerItem("reflector_thick",
        properties -> new ReactorReflectorItem(properties, 40_000, false));
    static final ReactorComponents REACTOR_COMPONENTS = new ReactorComponents(ITEMS);
    public static final EnrichedUranium ENRICHED_URANIUM = new EnrichedUranium(ITEMS);
    public static final DeferredItem<Item> TIN_INGOT = ITEMS.registerSimpleItem("ingot_tin");
    public static final DeferredItem<Item> TIN_DUST = ITEMS.registerSimpleItem("dust_tin");
    public static final DeferredItem<Item> ALUMINIUM_DUST = ITEMS.registerSimpleItem("dust_aluminium");
    public static final DeferredItem<Item> ALUMINIUM_INGOT = ITEMS.registerSimpleItem("ingot_aluminium");
    public static final DeferredItem<Item> BRONZE_INGOT = ITEMS.registerSimpleItem("ingot_bronze");
    public static final DeferredItem<Item> TURBINE_BLADE = ITEMS.registerSimpleItem("turbine_blade");
    public static final DeferredItem<Item> POTATO_SLICES = ITEMS.registerSimpleItem("potato_slices");
    public static final DeferredItem<Item> FRIES = ITEMS.registerSimpleItem("fries", new Item.Properties()
        .food(new FoodProperties.Builder().nutrition(1).saturationModifier(0.5F).alwaysEdible().build()));
    public static final DeferredItem<Item> ADVANCED_ALLOY = ITEMS.registerSimpleItem("ingot_advanced_alloy");
    public static final DeferredItem<Item> ADVANCED_ALLOY_PLATE = ITEMS.registerSimpleItem("plate_advanced_alloy");
    public static final DeferredItem<Item> CARBON_MESH = ITEMS.registerSimpleItem("carbon_mesh");
    public static final DeferredItem<Item> CARBON_PLATE = ITEMS.registerSimpleItem("carbon_plate");
    public static final DeferredItem<Item> RARE_EARTH_DUST = ITEMS.registerSimpleItem("dust_rare_earth");
    public static final DeferredItem<Item> RARE_EARTH_CHUNK = ITEMS.registerSimpleItem("rare_earth_chunk");
    public static final DeferredItem<Item> DEAD_MAGNET = ITEMS.registerSimpleItem("dead_magnet");
    public static final DeferredItem<Item> MAGNET = ITEMS.registerSimpleItem("magnet");
    public static final DeferredItem<FluidCellItem> CELL_EMPTY =
        ITEMS.registerItem("cell_empty", properties -> new FluidCellItem(properties, Fluids.EMPTY));
    public static final DeferredItem<Item> CELL_AIR =
        ITEMS.registerItem("cell_air", properties -> new Item(properties.craftRemainder(CELL_EMPTY.get())));
    public static final DeferredItem<FluidCellItem> CELL_WATER =
        ITEMS.registerItem("cell_water", properties -> new FluidCellItem(properties, Fluids.WATER));
    public static final DeferredItem<FluidCellItem> CELL_LAVA =
        ITEMS.registerItem("cell_lava", properties -> new FluidCellItem(properties, Fluids.LAVA));
    public static final DeferredItem<Item> ELECTROLYZED_WATER = ITEMS.registerSimpleItem("cell_electrolyzed_water");
    public static final DeferredItem<Item> SCRAP = ITEMS.registerItem("scrap", ScrapFuelItem::new);
    public static final DeferredItem<Item> SCRAP_METAL = ITEMS.registerSimpleItem("scrap_metal");
    public static final DeferredItem<Item> SCRAP_METAL_CHUNK = ITEMS.registerSimpleItem("scrap_metal_chunk");
    public static final DeferredItem<Item> SCRAP_METAL_BLADE = ITEMS.registerSimpleItem("scrap_metal_blade");
    public static final DeferredItem<Item> UPGRADE_BASE = ITEMS.registerSimpleItem("upgrade_base");
    public static final DeferredItem<Item> EFFICIENT_SAWBLADE = ITEMS.registerSimpleItem("efficient_sawblade", new Item.Properties().durability(1000));
    public static final DeferredItem<ScrapBoxItem> SCRAP_BOX = ITEMS.registerItem("scrapbox", ScrapBoxItem::new);
    public static final DeferredItem<Item> UU_MATTER = ITEMS.registerSimpleItem("uumatter");

    public IC2(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        Machines.register(modBus, ITEMS);
        AdvancedMachines.register(modBus, ITEMS);
        ExtraGenerators.register(modBus, ITEMS);
        PowerMachines.register(modBus, ITEMS);
        ic2.core.machine.ChargingDevices.register(modBus, ITEMS);
        Rubberwood.register(modBus, ITEMS);
        Materials.register(modBus, ITEMS);
        Hazmat.register(modBus, ITEMS);
        Radiation.register(modBus);
        CreativeTabs.register(modBus);
        modBus.addListener(IC2::registerCapabilities);
        modBus.addListener((net.neoforged.fml.event.lifecycle.FMLLoadCompleteEvent event) ->
            event.enqueueWork(ic2.api.internal.RecipeRegistration::close));
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(ScrapBoxItem::registerDispenser));
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ignored) -> new FluidCellItem.Handler(stack),
            CELL_EMPTY.get(), CELL_WATER.get(), CELL_LAVA.get());
    }
}
