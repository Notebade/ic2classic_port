package ic2.core.machine;

import ic2.core.IC2;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

final class ColossalMachines {
    enum Kind {
        FURNACE, MACERATOR, EXTRACTOR, COMPRESSOR, RECYCLER;
        String id() { return "colossal_" + name().toLowerCase(java.util.Locale.ROOT); }
    }
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IC2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IC2.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, IC2.MOD_ID);
    static final Map<Kind, DeferredBlock<ColossalBlock>> MACHINES = new EnumMap<>(Kind.class);
    static {
        for (var kind : Kind.values()) MACHINES.put(kind, BLOCKS.registerBlock(kind.id(), p -> new ColossalBlock(p, kind), PowerMachines.properties()));
    }
    static final DeferredBlock<ColossalPartBlock> BASE = BLOCKS.registerBlock("colossal_base", ColossalPartBlock::new, PowerMachines.properties());
    static final Supplier<BlockEntityType<ColossalEntity>> ENTITY = ENTITIES.register("colossal_machine", () ->
        BlockEntityType.Builder.of(ColossalEntity::new, MACHINES.values().stream().map(Supplier::get).toArray(Block[]::new)).build(null));
    static final Supplier<BlockEntityType<ColossalPartEntity>> PART_ENTITY = ENTITIES.register("colossal_part", () -> BlockEntityType.Builder.of(ColossalPartEntity::new, BASE.get()).build(null));
    static final Supplier<MenuType<ColossalMenu>> MENU = MENUS.register("colossal_machine", () -> IMenuTypeExtension.create((id, inventory, buffer) -> new ColossalMenu(id, inventory, buffer.readVarInt(), buffer.readEnum(Kind.class))));

    private ColossalMachines() {}
    static void register(IEventBus bus, DeferredRegister.Items items) {
        BLOCKS.register(bus); ENTITIES.register(bus); MENUS.register(bus);
        MACHINES.values().forEach(items::registerSimpleBlockItem); items.registerSimpleBlockItem(BASE);
        bus.addListener(ColossalMachines::capabilities);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY.get(), ColossalEntity::items);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ENTITY.get(), (machine, side) -> machine.fluids());
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PART_ENTITY.get(), ColossalPartEntity::items);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PART_ENTITY.get(), (part, side) -> part.fluids());
    }
}
