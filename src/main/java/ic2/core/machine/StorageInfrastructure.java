package ic2.core.machine;

import ic2.core.IC2;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Working storage dependencies of the original machine/personal tank recipes.
 */
public final class StorageInfrastructure {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IC2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IC2.MOD_ID);
    static final DeferredBlock<StorageBlock> BUFFER = BLOCKS.registerBlock("machine_buffer", (p)->new StorageBlock(p, false), PowerMachines.properties());
    static final DeferredBlock<StorageBlock> PERSONAL = BLOCKS.registerBlock("personal_chest", (p)->new StorageBlock(p, true), PowerMachines.properties().strength(3, 3600000));
    static final Supplier<BlockEntityType<StorageEntity>> ENTITY = ENTITIES.register("dependency_storage", ()->BlockEntityType.Builder.of(StorageEntity::new, BUFFER.get(), PERSONAL.get()).build(null));
    public static net.neoforged.neoforge.registries.DeferredItem<ToolboxItem> TOOLBOX;

    private StorageInfrastructure() {
    }

    static void register(IEventBus bus, DeferredRegister.Items items) {
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        items.registerSimpleBlockItem(BUFFER);
        items.registerSimpleBlockItem(PERSONAL);
        TOOLBOX = items.registerItem("toolbox", ToolboxItem::new, new net.minecraft.world.item.Item.Properties().stacksTo(1));
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event)->event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY.get(), (storage,side)->storage.personal() ? null : storage.inventory));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event)->{
            if (event.getLevel().getBlockEntity(event.getPos()) instanceof StorageEntity storage && !storage.allowed(event.getPlayer())) event.setCanceled(true);
        });
    }
}
