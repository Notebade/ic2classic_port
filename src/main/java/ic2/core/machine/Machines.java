package ic2.core.machine;

import ic2.core.IC2;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SimpleCookingSerializer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.items.wrapper.InvWrapper;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

public final class Machines {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IC2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IC2.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, IC2.MOD_ID);
    private static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
        DeferredRegister.create(Registries.RECIPE_TYPE, IC2.MOD_ID);
    private static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
        DeferredRegister.create(Registries.RECIPE_SERIALIZER, IC2.MOD_ID);
    // Keep the registry ID used by existing stone macerator datapacks.
    static final Supplier<RecipeType<MacerationRecipe>> MACERATING =
        RECIPE_TYPES.register("stone_macerating",
            () -> RecipeType.simple(ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, "stone_macerating")));
    static final Supplier<RecipeSerializer<MacerationRecipe>> MACERATING_SERIALIZER =
        RECIPE_SERIALIZERS.register("stone_macerating", () -> new SimpleCookingSerializer<>(MacerationRecipe::new, 400));
    static final Supplier<MenuType<StoneMaceratorMenu>> STONE_MACERATOR_MENU =
        MENUS.register("stone_macerator", () -> new MenuType<>(StoneMaceratorMenu::new, FeatureFlags.DEFAULT_FLAGS));
    static final Supplier<MenuType<MachineMenu>> MENU =
        MENUS.register("machine", () -> new MenuType<>(MachineMenu::new, FeatureFlags.DEFAULT_FLAGS));
    static final Supplier<MenuType<MachineMenu>> BATBOX_MENU =
        MENUS.register("batbox", () -> new MenuType<>(MachineMenu::batbox, FeatureFlags.DEFAULT_FLAGS));
    static final Supplier<MenuType<MachineMenu>> MFE_MENU =
        MENUS.register("mfe", () -> new MenuType<>(MachineMenu::mfe, FeatureFlags.DEFAULT_FLAGS));
    static final Supplier<MenuType<MachineMenu>> MFSU_MENU =
        MENUS.register("mfsu", () -> new MenuType<>(MachineMenu::mfsu, FeatureFlags.DEFAULT_FLAGS));
    static final Supplier<MenuType<MachineMenu>> ALLOY_SMELTER_MENU =
        MENUS.register("alloy_smelter", () -> new MenuType<>(MachineMenu::alloySmelter, FeatureFlags.DEFAULT_FLAGS));
    static final Supplier<MenuType<MachineMenu>> RARE_EARTH_MENU =
        MENUS.register("rare_earth_extractor", () -> new MenuType<>(MachineMenu::rareEarth, FeatureFlags.DEFAULT_FLAGS));
    static final Supplier<MenuType<MachineMenu>> ELECTROLYZER_MENU =
        MENUS.register("electrolyzer", () -> new MenuType<>(MachineMenu::electrolyzer, FeatureFlags.DEFAULT_FLAGS));
    static final Supplier<MenuType<MachineMenu>> FABRICATOR_MENU =
        MENUS.register("mass_fabricator", () -> new MenuType<>(MachineMenu::fabricator, FeatureFlags.DEFAULT_FLAGS));
    static final Supplier<MenuType<MachineMenu>> GEOTHERMAL_MENU =
        MENUS.register("geothermal_generator", () -> new MenuType<>(MachineMenu::geothermal, FeatureFlags.DEFAULT_FLAGS));

    static final DeferredBlock<MachineBlock> GENERATOR =
        BLOCKS.registerBlock("generator", properties -> new MachineBlock(properties, MachineKind.GENERATOR), machineProperties());
    static final DeferredBlock<GeothermalGeneratorBlock> GEOTHERMAL =
        BLOCKS.registerBlock("geothermal_generator", GeothermalGeneratorBlock::new, machineProperties());
    static final DeferredBlock<SolarPanelBlock> SOLAR_PANEL =
        BLOCKS.registerBlock("solar_panel", properties -> new SolarPanelBlock(properties, SolarPanelBlock.BASE_OUTPUT),
            machineProperties());
    static final DeferredBlock<CompressedSolarPanelBlock> COMPRESSED_SOLAR_PANEL =
        BLOCKS.registerBlock("solar_panel_compressed",
            properties -> new CompressedSolarPanelBlock(properties, SolarPanelBlock.BASE_OUTPUT), machineProperties().noOcclusion());
    static final DeferredBlock<SolarPanelBlock> LV_SOLAR_PANEL =
        BLOCKS.registerBlock("solar_panel_lv", properties -> new SolarPanelBlock(properties, SolarPanelBlock.LV_OUTPUT),
            machineProperties());
    static final DeferredBlock<CompressedSolarPanelBlock> COMPRESSED_LV_SOLAR_PANEL =
        BLOCKS.registerBlock("solar_panel_lv_compressed",
            properties -> new CompressedSolarPanelBlock(properties, SolarPanelBlock.LV_OUTPUT), machineProperties().noOcclusion());
    static final DeferredBlock<SolarPanelBlock> MV_SOLAR_PANEL =
        BLOCKS.registerBlock("solar_panel_mv", properties -> new SolarPanelBlock(properties, SolarPanelBlock.MV_OUTPUT),
            machineProperties());
    static final DeferredBlock<CompressedSolarPanelBlock> COMPRESSED_MV_SOLAR_PANEL =
        BLOCKS.registerBlock("solar_panel_mv_compressed",
            properties -> new CompressedSolarPanelBlock(properties, SolarPanelBlock.MV_OUTPUT), machineProperties().noOcclusion());
    static final DeferredBlock<TransformerBlock> LV_TRANSFORMER =
        BLOCKS.registerBlock("transformer_lv",
            properties -> new TransformerBlock(properties, MachineKind.LV_TRANSFORMER, TransformerBlock.LV_HIGH_PACKET),
            machineProperties());
    static final DeferredBlock<TransformerBlock> MV_TRANSFORMER =
        BLOCKS.registerBlock("transformer_mv",
            properties -> new TransformerBlock(properties, MachineKind.MV_TRANSFORMER, TransformerBlock.MV_HIGH_PACKET),
            machineProperties());
    static final DeferredBlock<MachineBlock> MACERATOR =
        BLOCKS.registerBlock("macerator", properties -> new MachineBlock(properties, MachineKind.MACERATOR), machineProperties());
    static final DeferredBlock<MachineBlock> ELECTRIC_FURNACE =
        BLOCKS.registerBlock("electric_furnace", properties -> new MachineBlock(properties, MachineKind.ELECTRIC_FURNACE),
            machineProperties());
    static final DeferredBlock<ActiveMachineBlock> ELECTRIC_BLAST_FURNACE =
        BLOCKS.registerBlock("electric_blast_furnace",
            properties -> new ActiveMachineBlock(properties, MachineKind.ELECTRIC_BLAST_FURNACE), machineProperties());
    static final DeferredBlock<ActiveMachineBlock> ELECTRIC_SMOKER =
        BLOCKS.registerBlock("electric_smoker",
            properties -> new ActiveMachineBlock(properties, MachineKind.ELECTRIC_SMOKER), machineProperties());
    static final DeferredBlock<ActiveMachineBlock> SAWMILL =
        BLOCKS.registerBlock("sawmill", properties -> new ActiveMachineBlock(properties, MachineKind.SAWMILL),
            machineProperties());
    static final DeferredBlock<IronFurnaceBlock> IRON_FURNACE =
        BLOCKS.registerBlock("iron_furnace", IronFurnaceBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE));
    static final DeferredBlock<StoneMaceratorBlock> STONE_MACERATOR =
        BLOCKS.registerBlock("stone_macerator", StoneMaceratorBlock::new, BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE));
    static final DeferredBlock<EnergyStorageBlock> BATBOX =
        BLOCKS.registerBlock("batbox", properties -> new EnergyStorageBlock(properties, MachineKind.BATBOX), machineProperties());
    static final DeferredBlock<EnergyStorageBlock> MFE =
        BLOCKS.registerBlock("mfe", properties -> new EnergyStorageBlock(properties, MachineKind.MFE), machineProperties());
    static final DeferredBlock<EnergyStorageBlock> MFSU =
        BLOCKS.registerBlock("mfsu", properties -> new EnergyStorageBlock(properties, MachineKind.MFSU), machineProperties());
    static final DeferredBlock<ActiveMachineBlock> ALLOY_SMELTER =
        BLOCKS.registerBlock("alloy_smelter", properties -> new ActiveMachineBlock(properties, MachineKind.ALLOY_SMELTER),
            machineProperties());
    static final DeferredBlock<ActiveMachineBlock> RARE_EARTH_EXTRACTOR =
        BLOCKS.registerBlock("rare_earth_extractor",
            properties -> new ActiveMachineBlock(properties, MachineKind.RARE_EARTH_EXTRACTOR), machineProperties());
    static final DeferredBlock<ActiveMachineBlock> ELECTROLYZER =
        BLOCKS.registerBlock("electrolyzer",
            properties -> new ActiveMachineBlock(properties, MachineKind.ELECTROLYZER), machineProperties());
    static final DeferredBlock<MachineBlock> EXTRACTOR =
        BLOCKS.registerBlock("extractor", properties -> new MachineBlock(properties, MachineKind.EXTRACTOR), machineProperties());
    static final DeferredBlock<MachineBlock> COMPRESSOR =
        BLOCKS.registerBlock("compressor", properties -> new MachineBlock(properties, MachineKind.COMPRESSOR),
            machineProperties());
    static final DeferredBlock<MachineBlock> PUMP =
        BLOCKS.registerBlock("pump", properties -> new MachineBlock(properties, MachineKind.PUMP), machineProperties());
    static final DeferredBlock<MachineBlock> RECYCLER =
        BLOCKS.registerBlock("recycler", properties -> new MachineBlock(properties, MachineKind.RECYCLER),
            machineProperties());
    static final DeferredBlock<MachineBlock> MASS_FABRICATOR =
        BLOCKS.registerBlock("mass_fabricator", properties -> new MachineBlock(properties, MachineKind.MASS_FABRICATOR),
            machineProperties());
    static final DeferredBlock<EnergyCableBlock> COPPER_CABLE = cable("copper_cable", 300, Integer.MAX_VALUE);
    static final DeferredBlock<EnergyCableBlock> INSULATED_COPPER_CABLE = cable("copper_insulated_cable", 200, Integer.MAX_VALUE);
    static final DeferredBlock<EnergyCableBlock> GOLD_CABLE = cable("gold_cable", 500, Integer.MAX_VALUE);
    static final DeferredBlock<EnergyCableBlock> INSULATED_GOLD_CABLE = cable("gold_insulated_cable", 450, Integer.MAX_VALUE);
    static final DeferredBlock<EnergyCableBlock> DOUBLE_INSULATED_GOLD_CABLE = cable("gold_double_insulated_cable", 400, Integer.MAX_VALUE);
    static final DeferredBlock<EnergyCableBlock> GLASSFIBER_CABLE = cable("glassfiber_cable", 25, 512);

    static final Supplier<BlockEntityType<MachineEntity>> GENERATOR_ENTITY =
        ENTITIES.register("generator", () -> entityType(GENERATOR.get()));
    static final Supplier<BlockEntityType<MachineEntity>> GEOTHERMAL_ENTITY =
        ENTITIES.register("geothermal_generator", () -> entityType(GEOTHERMAL.get()));
    static final Supplier<BlockEntityType<MachineEntity>> SOLAR_PANEL_ENTITY =
        ENTITIES.register("solar_panel", () -> entityType(SOLAR_PANEL.get(), COMPRESSED_SOLAR_PANEL.get(),
            LV_SOLAR_PANEL.get(), COMPRESSED_LV_SOLAR_PANEL.get(), MV_SOLAR_PANEL.get(), COMPRESSED_MV_SOLAR_PANEL.get()));
    static final Supplier<BlockEntityType<MachineEntity>> LV_TRANSFORMER_ENTITY =
        ENTITIES.register("transformer_lv", () -> entityType(LV_TRANSFORMER.get()));
    static final Supplier<BlockEntityType<MachineEntity>> MV_TRANSFORMER_ENTITY =
        ENTITIES.register("transformer_mv", () -> entityType(MV_TRANSFORMER.get()));
    static final Supplier<BlockEntityType<MachineEntity>> MACERATOR_ENTITY =
        ENTITIES.register("macerator", () -> entityType(MACERATOR.get()));
    static final Supplier<BlockEntityType<MachineEntity>> ELECTRIC_FURNACE_ENTITY =
        ENTITIES.register("electric_furnace", () -> entityType(ELECTRIC_FURNACE.get()));
    static final Supplier<BlockEntityType<MachineEntity>> ELECTRIC_BLAST_FURNACE_ENTITY =
        ENTITIES.register("electric_blast_furnace", () -> entityType(ELECTRIC_BLAST_FURNACE.get()));
    static final Supplier<BlockEntityType<MachineEntity>> ELECTRIC_SMOKER_ENTITY =
        ENTITIES.register("electric_smoker", () -> entityType(ELECTRIC_SMOKER.get()));
    static final Supplier<BlockEntityType<MachineEntity>> SAWMILL_ENTITY =
        ENTITIES.register("sawmill", () -> entityType(SAWMILL.get()));
    static final Supplier<BlockEntityType<IronFurnaceBlockEntity>> IRON_FURNACE_ENTITY =
        ENTITIES.register("iron_furnace", () -> BlockEntityType.Builder.of(IronFurnaceBlockEntity::new, IRON_FURNACE.get()).build(null));
    static final Supplier<BlockEntityType<StoneMaceratorBlockEntity>> STONE_MACERATOR_ENTITY =
        ENTITIES.register("stone_macerator",
            () -> BlockEntityType.Builder.of(StoneMaceratorBlockEntity::new, STONE_MACERATOR.get()).build(null));
    static final Supplier<BlockEntityType<MachineEntity>> BATBOX_ENTITY =
        ENTITIES.register("batbox", () -> entityType(BATBOX.get()));
    static final Supplier<BlockEntityType<MachineEntity>> MFE_ENTITY =
        ENTITIES.register("mfe", () -> entityType(MFE.get()));
    static final Supplier<BlockEntityType<MachineEntity>> MFSU_ENTITY =
        ENTITIES.register("mfsu", () -> entityType(MFSU.get()));
    static final Supplier<BlockEntityType<MachineEntity>> ALLOY_SMELTER_ENTITY =
        ENTITIES.register("alloy_smelter", () -> entityType(ALLOY_SMELTER.get()));
    static final Supplier<BlockEntityType<MachineEntity>> RARE_EARTH_ENTITY =
        ENTITIES.register("rare_earth_extractor", () -> entityType(RARE_EARTH_EXTRACTOR.get()));
    static final Supplier<BlockEntityType<MachineEntity>> ELECTROLYZER_ENTITY =
        ENTITIES.register("electrolyzer", () -> entityType(ELECTROLYZER.get()));
    static final Supplier<BlockEntityType<MachineEntity>> EXTRACTOR_ENTITY =
        ENTITIES.register("extractor", () -> entityType(EXTRACTOR.get()));
    static final Supplier<BlockEntityType<MachineEntity>> COMPRESSOR_ENTITY =
        ENTITIES.register("compressor", () -> entityType(COMPRESSOR.get()));
    static final Supplier<BlockEntityType<MachineEntity>> PUMP_ENTITY =
        ENTITIES.register("pump", () -> entityType(PUMP.get()));
    static final Supplier<BlockEntityType<MachineEntity>> RECYCLER_ENTITY =
        ENTITIES.register("recycler", () -> entityType(RECYCLER.get()));
    static final Supplier<BlockEntityType<MachineEntity>> MASS_FABRICATOR_ENTITY =
        ENTITIES.register("mass_fabricator", () -> entityType(MASS_FABRICATOR.get()));

    public static void register(IEventBus bus, DeferredRegister.Items items) {
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        RECIPE_TYPES.register(bus);
        RECIPE_SERIALIZERS.register(bus);
        bus.addListener(Machines::registerCapabilities);
        items.registerSimpleBlockItem(GENERATOR);
        items.registerSimpleBlockItem(GEOTHERMAL);
        items.registerSimpleBlockItem(SOLAR_PANEL);
        items.registerSimpleBlockItem(COMPRESSED_SOLAR_PANEL);
        items.registerSimpleBlockItem(LV_SOLAR_PANEL);
        items.registerSimpleBlockItem(COMPRESSED_LV_SOLAR_PANEL);
        items.registerSimpleBlockItem(MV_SOLAR_PANEL);
        items.registerSimpleBlockItem(COMPRESSED_MV_SOLAR_PANEL);
        items.registerSimpleBlockItem(LV_TRANSFORMER);
        items.registerSimpleBlockItem(MV_TRANSFORMER);
        items.registerSimpleBlockItem(MACERATOR);
        items.registerSimpleBlockItem(ELECTRIC_FURNACE);
        items.registerSimpleBlockItem(ELECTRIC_BLAST_FURNACE);
        items.registerSimpleBlockItem(ELECTRIC_SMOKER);
        items.registerSimpleBlockItem(SAWMILL);
        items.registerSimpleBlockItem(IRON_FURNACE);
        items.registerSimpleBlockItem(STONE_MACERATOR);
        items.registerSimpleBlockItem(BATBOX);
        items.registerSimpleBlockItem(MFE);
        items.registerSimpleBlockItem(MFSU);
        items.registerSimpleBlockItem(ALLOY_SMELTER);
        items.registerSimpleBlockItem(RARE_EARTH_EXTRACTOR);
        items.registerSimpleBlockItem(ELECTROLYZER);
        items.registerSimpleBlockItem(EXTRACTOR);
        items.registerSimpleBlockItem(COMPRESSOR);
        items.registerSimpleBlockItem(PUMP);
        items.registerSimpleBlockItem(RECYCLER);
        items.registerSimpleBlockItem(MASS_FABRICATOR);
        var copperCableItem = items.registerSimpleBlockItem("copper_cable_item", COPPER_CABLE);
        var insulatedCopperCableItem = items.registerSimpleBlockItem("copper_insulated_cable_item", INSULATED_COPPER_CABLE);
        var goldCableItem = items.registerSimpleBlockItem("gold_cable_item", GOLD_CABLE);
        var insulatedGoldCableItem = items.registerSimpleBlockItem("gold_insulated_cable_item", INSULATED_GOLD_CABLE);
        var doubleInsulatedGoldCableItem = items.registerSimpleBlockItem("gold_double_insulated_cable_item", DOUBLE_INSULATED_GOLD_CABLE);
        var glassfiberCableItem = items.registerSimpleBlockItem("glassfiber_cable_item", GLASSFIBER_CABLE);
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.REDSTONE_BLOCKS)) {
                event.accept(GENERATOR.get());
                event.accept(GEOTHERMAL.get());
                event.accept(SOLAR_PANEL.get());
                event.accept(COMPRESSED_SOLAR_PANEL.get());
                event.accept(LV_SOLAR_PANEL.get());
                event.accept(COMPRESSED_LV_SOLAR_PANEL.get());
                event.accept(MV_SOLAR_PANEL.get());
                event.accept(COMPRESSED_MV_SOLAR_PANEL.get());
                event.accept(LV_TRANSFORMER.get());
                event.accept(MV_TRANSFORMER.get());
                event.accept(copperCableItem.get());
                event.accept(insulatedCopperCableItem.get());
                event.accept(goldCableItem.get());
                event.accept(insulatedGoldCableItem.get());
                event.accept(doubleInsulatedGoldCableItem.get());
                event.accept(glassfiberCableItem.get());
                event.accept(MACERATOR.get());
                event.accept(ELECTRIC_FURNACE.get());
                event.accept(ELECTRIC_BLAST_FURNACE.get());
                event.accept(ELECTRIC_SMOKER.get());
                event.accept(SAWMILL.get());
                event.accept(IRON_FURNACE.get());
                event.accept(STONE_MACERATOR.get());
                event.accept(BATBOX.get());
                event.accept(MFE.get());
                event.accept(MFSU.get());
                event.accept(ALLOY_SMELTER.get());
                event.accept(RARE_EARTH_EXTRACTOR.get());
                event.accept(ELECTROLYZER.get());
                event.accept(EXTRACTOR.get());
                event.accept(COMPRESSOR.get());
                event.accept(PUMP.get());
                event.accept(RECYCLER.get());
                event.accept(MASS_FABRICATOR.get());
                event.accept(IC2.SCRAP_BOX.get());
                event.accept(IC2.RE_BATTERY.get());
                event.accept(IC2.ENERGY_CRYSTAL.get());
                event.accept(IC2.LAPOTRON_CRYSTAL.get());
            }
        });
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GENERATOR_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GEOTHERMAL_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, GEOTHERMAL_ENTITY.get(), MachineEntity::geothermalFluid);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MACERATOR_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ELECTRIC_FURNACE_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ELECTRIC_BLAST_FURNACE_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ELECTRIC_SMOKER_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, SAWMILL_ENTITY.get(), MachineEntity::automation);
        registerFuelInventory(event, IRON_FURNACE_ENTITY.get());
        registerFuelInventory(event, STONE_MACERATOR_ENTITY.get());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BATBOX_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MFE_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MFSU_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ALLOY_SMELTER_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RARE_EARTH_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ELECTROLYZER_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, EXTRACTOR_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, COMPRESSOR_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PUMP_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PUMP_ENTITY.get(), MachineEntity::fluidOutput);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, RECYCLER_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MASS_FABRICATOR_ENTITY.get(), MachineEntity::automation);
    }

    private static void registerFuelInventory(RegisterCapabilitiesEvent event,
            BlockEntityType<? extends AbstractFurnaceBlockEntity> type) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type,
            (entity, side) -> side == null ? new InvWrapper(entity) : new SidedInvWrapper(entity, side));
    }

    private static BlockEntityType<MachineEntity> entityType(Block... blocks) {
        return BlockEntityType.Builder.of(MachineEntity::new, blocks).build(null);
    }

    private static DeferredBlock<EnergyCableBlock> cable(String name, int lossMilliEu, int maxPacket) {
        return BLOCKS.registerBlock(name, properties -> new EnergyCableBlock(properties, lossMilliEu, maxPacket),
            BlockBehaviour.Properties.of().strength(0.4F).noOcclusion());
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of().strength(3.5F);
    }
}
