package ic2.core;

import com.mojang.serialization.Codec;
import ic2.core.item.FluidCellItem;
import ic2.core.item.RechargeableItem;
import ic2.core.item.ScrapBoxItem;
import ic2.core.item.UraniumFuelRod;
import ic2.core.machine.Machines;
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
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<UuMatterRecipe>> UU_MATTER_RECIPE =
        RECIPE_SERIALIZERS.register("uu_matter", () -> UuMatterRecipe.SERIALIZER);
    private static final DeferredRegister.DataComponents COMPONENTS =
        DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, MOD_ID);
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> BATTERY_CHARGE =
        COMPONENTS.registerComponentType("battery_charge", builder -> builder
            .persistent(Codec.intRange(0, LAPOTRON_CAPACITY)));
    public static final DeferredItem<RechargeableItem> RE_BATTERY =
        ITEMS.registerItem("re_battery", properties -> new RechargeableItem(properties, 10_000, 100, 32),
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<RechargeableItem> ENERGY_CRYSTAL =
        ITEMS.registerItem("energy_crystal", properties -> new RechargeableItem(properties, 100_000, 250, 128),
            new Item.Properties().stacksTo(1));
    public static final DeferredItem<RechargeableItem> LAPOTRON_CRYSTAL =
        ITEMS.registerItem("lapatron_crystal", properties -> new RechargeableItem(properties, LAPOTRON_CAPACITY, 600, 512),
            new Item.Properties().stacksTo(1));
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
    public static final DeferredItem<Item> NEUTRON_REFLECTOR = ITEMS.registerSimpleItem("reflector",
        new Item.Properties().durability(10_000).setNoRepair());
    public static final DeferredItem<Item> THICK_NEUTRON_REFLECTOR = ITEMS.registerSimpleItem("reflector_thick",
        new Item.Properties().durability(40_000).setNoRepair());
    static final ReactorComponents REACTOR_COMPONENTS = new ReactorComponents(ITEMS);
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
    public static final DeferredItem<FluidCellItem> CELL_WATER =
        ITEMS.registerItem("cell_water", properties -> new FluidCellItem(properties, Fluids.WATER));
    public static final DeferredItem<FluidCellItem> CELL_LAVA =
        ITEMS.registerItem("cell_lava", properties -> new FluidCellItem(properties, Fluids.LAVA));
    public static final DeferredItem<Item> ELECTROLYZED_WATER = ITEMS.registerSimpleItem("cell_electrolyzed_water");
    public static final DeferredItem<Item> SCRAP = ITEMS.registerSimpleItem("scrap");
    public static final DeferredItem<Item> SCRAP_METAL = ITEMS.registerSimpleItem("scrap_metal");
    public static final DeferredItem<ScrapBoxItem> SCRAP_BOX = ITEMS.registerItem("scrapbox", ScrapBoxItem::new);
    public static final DeferredItem<Item> UU_MATTER = ITEMS.registerSimpleItem("uumatter");

    public IC2(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        Machines.register(modBus, ITEMS);
        Rubberwood.register(modBus, ITEMS);
        Materials.register(modBus, ITEMS);
        Hazmat.register(modBus, ITEMS);
        Radiation.register(modBus);
        modBus.addListener(IC2::registerCapabilities);
        modBus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(ScrapBoxItem::registerDispenser));
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerItem(Capabilities.FluidHandler.ITEM, (stack, ignored) -> new FluidCellItem.Handler(stack),
            CELL_EMPTY.get(), CELL_WATER.get(), CELL_LAVA.get());
    }
}
