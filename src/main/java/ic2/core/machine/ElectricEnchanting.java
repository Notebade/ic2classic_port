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

final class ElectricEnchanting {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IC2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IC2.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, IC2.MOD_ID);
    static final DeferredBlock<ElectricEnchanterBlock> BLOCK = BLOCKS.registerBlock("electric_enchanter", ElectricEnchanterBlock::new, PowerMachines.properties());
    static final Supplier<BlockEntityType<ElectricEnchanterEntity>> ENTITY = ENTITIES.register("electric_enchanter", () -> BlockEntityType.Builder.of(ElectricEnchanterEntity::new, BLOCK.get()).build(null));
    static final Supplier<MenuType<ElectricEnchanterMenu>> MENU = MENUS.register("electric_enchanter", () -> new MenuType<>(ElectricEnchanterMenu::new, FeatureFlags.DEFAULT_FLAGS));
    private ElectricEnchanting() {}
    static void register(IEventBus bus, DeferredRegister.Items items) {
        BLOCKS.register(bus); ENTITIES.register(bus); MENUS.register(bus); items.registerSimpleBlockItem(BLOCK);
        bus.addListener((RegisterCapabilitiesEvent event) -> event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY.get(), ElectricEnchanterEntity::automation));
    }
}
