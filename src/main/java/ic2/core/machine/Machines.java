package ic2.core.machine;

import ic2.core.IC2;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class Machines {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IC2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES =
        DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IC2.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, IC2.MOD_ID);
    static final Supplier<MenuType<MachineMenu>> MENU =
        MENUS.register("machine", () -> new MenuType<>(MachineMenu::new, FeatureFlags.DEFAULT_FLAGS));
    static final Supplier<MenuType<MachineMenu>> BATBOX_MENU =
        MENUS.register("batbox", () -> new MenuType<>(MachineMenu::batbox, FeatureFlags.DEFAULT_FLAGS));

    static final DeferredBlock<MachineBlock> GENERATOR =
        BLOCKS.registerBlock("generator", properties -> new MachineBlock(properties, MachineKind.GENERATOR), machineProperties());
    static final DeferredBlock<SolarPanelBlock> SOLAR_PANEL =
        BLOCKS.registerBlock("solar_panel", SolarPanelBlock::new, machineProperties());
    static final DeferredBlock<MachineBlock> MACERATOR =
        BLOCKS.registerBlock("macerator", properties -> new MachineBlock(properties, MachineKind.MACERATOR), machineProperties());
    static final DeferredBlock<MachineBlock> ELECTRIC_FURNACE =
        BLOCKS.registerBlock("electric_furnace", properties -> new MachineBlock(properties, MachineKind.ELECTRIC_FURNACE),
            machineProperties());
    static final DeferredBlock<BatBoxBlock> BATBOX =
        BLOCKS.registerBlock("batbox", BatBoxBlock::new, machineProperties());
    static final DeferredBlock<MachineBlock> EXTRACTOR =
        BLOCKS.registerBlock("extractor", properties -> new MachineBlock(properties, MachineKind.EXTRACTOR), machineProperties());
    static final DeferredBlock<MachineBlock> COMPRESSOR =
        BLOCKS.registerBlock("compressor", properties -> new MachineBlock(properties, MachineKind.COMPRESSOR),
            machineProperties());
    static final DeferredBlock<MachineBlock> PUMP =
        BLOCKS.registerBlock("pump", properties -> new MachineBlock(properties, MachineKind.PUMP), machineProperties());
    static final DeferredBlock<CopperCableBlock> COPPER_CABLE =
        BLOCKS.registerBlock("copper_cable", CopperCableBlock::new, BlockBehaviour.Properties.of().strength(0.4F).noOcclusion());

    static final Supplier<BlockEntityType<MachineEntity>> GENERATOR_ENTITY =
        ENTITIES.register("generator", () -> entityType(GENERATOR.get()));
    static final Supplier<BlockEntityType<MachineEntity>> SOLAR_PANEL_ENTITY =
        ENTITIES.register("solar_panel", () -> entityType(SOLAR_PANEL.get()));
    static final Supplier<BlockEntityType<MachineEntity>> MACERATOR_ENTITY =
        ENTITIES.register("macerator", () -> entityType(MACERATOR.get()));
    static final Supplier<BlockEntityType<MachineEntity>> ELECTRIC_FURNACE_ENTITY =
        ENTITIES.register("electric_furnace", () -> entityType(ELECTRIC_FURNACE.get()));
    static final Supplier<BlockEntityType<MachineEntity>> BATBOX_ENTITY =
        ENTITIES.register("batbox", () -> entityType(BATBOX.get()));
    static final Supplier<BlockEntityType<MachineEntity>> EXTRACTOR_ENTITY =
        ENTITIES.register("extractor", () -> entityType(EXTRACTOR.get()));
    static final Supplier<BlockEntityType<MachineEntity>> COMPRESSOR_ENTITY =
        ENTITIES.register("compressor", () -> entityType(COMPRESSOR.get()));
    static final Supplier<BlockEntityType<MachineEntity>> PUMP_ENTITY =
        ENTITIES.register("pump", () -> entityType(PUMP.get()));

    public static void register(IEventBus bus, DeferredRegister.Items items) {
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        bus.addListener(Machines::registerCapabilities);
        items.registerSimpleBlockItem(GENERATOR);
        items.registerSimpleBlockItem(SOLAR_PANEL);
        items.registerSimpleBlockItem(MACERATOR);
        items.registerSimpleBlockItem(ELECTRIC_FURNACE);
        items.registerSimpleBlockItem(BATBOX);
        items.registerSimpleBlockItem(EXTRACTOR);
        items.registerSimpleBlockItem(COMPRESSOR);
        items.registerSimpleBlockItem(PUMP);
        var copperCableItem = items.registerSimpleBlockItem("copper_cable_item", COPPER_CABLE);
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.REDSTONE_BLOCKS)) {
                event.accept(GENERATOR.get());
                event.accept(SOLAR_PANEL.get());
                event.accept(copperCableItem.get());
                event.accept(MACERATOR.get());
                event.accept(ELECTRIC_FURNACE.get());
                event.accept(BATBOX.get());
                event.accept(EXTRACTOR.get());
                event.accept(COMPRESSOR.get());
                event.accept(PUMP.get());
                event.accept(IC2.RE_BATTERY.get());
            }
        });
    }

    private static void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, GENERATOR_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MACERATOR_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ELECTRIC_FURNACE_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, BATBOX_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, EXTRACTOR_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, COMPRESSOR_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PUMP_ENTITY.get(), MachineEntity::automation);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PUMP_ENTITY.get(), MachineEntity::fluidOutput);
    }

    private static BlockEntityType<MachineEntity> entityType(Block block) {
        return BlockEntityType.Builder.of(MachineEntity::new, block).build(null);
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of().strength(3.5F);
    }
}
