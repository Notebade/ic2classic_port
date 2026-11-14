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
    private static final DeferredRegister<net.minecraft.world.inventory.MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, IC2.MOD_ID);
    static final Supplier<net.minecraft.world.inventory.MenuType<net.minecraft.world.inventory.ChestMenu>> BUFFER_MENU = chest("machine_buffer", 1);
    static final Supplier<net.minecraft.world.inventory.MenuType<net.minecraft.world.inventory.ChestMenu>> PERSONAL_MENU = chest("personal_chest", 6);
    static final Supplier<net.minecraft.world.inventory.MenuType<ToolboxMenu>> TOOLBOX_MENU = toolbox("toolbox", 8);
    static final Supplier<net.minecraft.world.inventory.MenuType<ToolboxMenu>> CARBON_MENU = toolbox("carbon_box", 15);
    static final Supplier<net.minecraft.world.inventory.MenuType<ToolboxMenu>> IRIDIUM_MENU = toolbox("iridium_box", 45);
    private static Supplier<net.minecraft.world.inventory.MenuType<net.minecraft.world.inventory.ChestMenu>> chest(String name, int rows) {
        return MENUS.register(name, () -> new net.minecraft.world.inventory.MenuType<>((id, player) ->
            new net.minecraft.world.inventory.ChestMenu(rows == 1 ? BUFFER_MENU.get() : PERSONAL_MENU.get(), id, player,
                new net.minecraft.world.SimpleContainer(rows * 9), rows), net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    }
    private static Supplier<net.minecraft.world.inventory.MenuType<ToolboxMenu>> toolbox(String name, int capacity) {
        return MENUS.register(name, () -> new net.minecraft.world.inventory.MenuType<>((id, player) ->
            new ToolboxMenu(id, player, new net.minecraft.world.SimpleContainer(capacity == 8 ? 9 : capacity == 15 ? 18 : 45),
                net.minecraft.world.item.ItemStack.EMPTY, capacity), net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS));
    }
    static final DeferredBlock<StorageBlock> BUFFER = BLOCKS.registerBlock("machine_buffer", (p)->new StorageBlock(p, false), PowerMachines.properties());
    static final DeferredBlock<StorageBlock> PERSONAL = BLOCKS.registerBlock("personal_chest", (p)->new StorageBlock(p, true), PowerMachines.properties().strength(3, 3600000));
    static final Supplier<BlockEntityType<StorageEntity>> ENTITY = ENTITIES.register("dependency_storage", ()->BlockEntityType.Builder.of(StorageEntity::new, BUFFER.get(), PERSONAL.get()).build(null));
    public static net.neoforged.neoforge.registries.DeferredItem<ToolboxItem> TOOLBOX;

    private StorageInfrastructure() {
    }

    static void register(IEventBus bus, DeferredRegister.Items items) {
        MENUS.register(bus);
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        items.registerSimpleBlockItem(BUFFER);
        items.registerSimpleBlockItem(PERSONAL);
        TOOLBOX = items.registerItem("toolbox", ToolboxItem::new, new net.minecraft.world.item.Item.Properties().stacksTo(1));
        items.registerItem("carbon_box", p -> new ToolboxItem(p, 15));
        items.registerItem("iridium_box", p -> new ToolboxItem(p, 45));
        bus.addListener((net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent event)->event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY.get(), (storage,side)->storage.personal() ? null : storage.inventory));
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.level.BlockEvent.BreakEvent event)->{
            if (event.getLevel().getBlockEntity(event.getPos()) instanceof StorageEntity storage && !storage.allowed(event.getPlayer())) event.setCanceled(true);
        });
    }
}
