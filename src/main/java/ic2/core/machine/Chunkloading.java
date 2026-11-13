package ic2.core.machine;

import ic2.core.IC2;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.chunk.RegisterTicketControllersEvent;
import net.neoforged.neoforge.common.world.chunk.TicketController;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

final class Chunkloading {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IC2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IC2.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, IC2.MOD_ID);
    static final DeferredBlock<ChunkloaderBlock> BLOCK = BLOCKS.registerBlock("chunkloader", ChunkloaderBlock::new, PowerMachines.properties());
    static final Supplier<BlockEntityType<ChunkloaderEntity>> ENTITY = ENTITIES.register("chunkloader", () -> BlockEntityType.Builder.of(ChunkloaderEntity::new, BLOCK.get()).build(null));
    static final Supplier<MenuType<ChunkloaderMenu>> MENU = MENUS.register("chunkloader", () -> new MenuType<>(ChunkloaderMenu::new, FeatureFlags.DEFAULT_FLAGS));
    static final TicketController TICKETS = new TicketController(ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, "chunkloader"), (level, helper) -> {
        for (var owner : helper.getBlockTickets().keySet()) {
            level.getChunkAt(owner);
            if (!(level.getBlockEntity(owner) instanceof ChunkloaderEntity entity) || !entity.savedActive()) helper.removeAllTickets(owner);
        }
    });

    private Chunkloading() {}

    static void register(IEventBus bus, DeferredRegister.Items items) {
        BLOCKS.register(bus); ENTITIES.register(bus); MENUS.register(bus); items.registerSimpleBlockItem(BLOCK);
        bus.addListener((RegisterTicketControllersEvent event) -> event.register(TICKETS));
    }
}
