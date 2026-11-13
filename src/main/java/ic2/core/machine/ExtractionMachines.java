package ic2.core.machine;

import ic2.core.IC2;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

final class ExtractionMachines {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IC2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IC2.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, IC2.MOD_ID);
    static final DeferredBlock<ExtractionBlock> FAST_PUMP = BLOCKS.registerBlock("overclocked_pump", ExtractionBlock::new, PowerMachines.properties());
    static final DeferredBlock<MinerBlock> MINER = BLOCKS.registerBlock("miner", MinerBlock::new, PowerMachines.properties());
    static final DeferredBlock<MiningPipeBlock> PIPE_TIP = BLOCKS.registerBlock("miningpipe", p -> new MiningPipeBlock(p, false), PowerMachines.properties().noOcclusion());
    static final Supplier<BlockEntityType<MinerEntity>> MINER_ENTITY = ENTITIES.register("miner", () -> BlockEntityType.Builder.of(MinerEntity::new, MINER.get()).build(null));
    static final Supplier<MenuType<ExtractionMenu>> MINER_MENU = MENUS.register("miner", () -> new MenuType<>((id, player) ->
        new ExtractionMenu(ExtractionMachines.MINER_MENU.get(), id, player, null, new net.neoforged.neoforge.items.ItemStackHandler(4), new net.minecraft.world.inventory.SimpleContainerData(6), -1), FeatureFlags.DEFAULT_FLAGS));
    static final Supplier<BlockEntityType<ExtractionEntity>> ENTITY = ENTITIES.register("extraction_machine", () -> BlockEntityType.Builder.of(ExtractionEntity::new, FAST_PUMP.get()).build(null));
    static final Supplier<MenuType<ExtractionMenu>> MENU = MENUS.register("extraction_machine", () -> new MenuType<>(ExtractionMenu::new, FeatureFlags.DEFAULT_FLAGS));
    static void register(IEventBus bus, DeferredRegister.Items items) {
        MiningTools.register(items);
        BLOCKS.register(bus); ENTITIES.register(bus); MENUS.register(bus); items.registerSimpleBlockItem(FAST_PUMP);
        items.registerSimpleBlockItem(MINER);
        bus.addListener(ExtractionMachines::capabilities);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY.get(), ExtractionEntity::automation);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ENTITY.get(), ExtractionEntity::fluidOutput);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MINER_ENTITY.get(), MinerEntity::automation);
    }
}
