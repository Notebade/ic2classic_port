package ic2.core.machine;

import ic2.core.IC2;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NuclearInfrastructure {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IC2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IC2.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, IC2.MOD_ID);
    private static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUND_EVENTS = DeferredRegister.create(Registries.SOUND_EVENT, IC2.MOD_ID);
    static final java.util.Map<String, Supplier<net.minecraft.sounds.SoundEvent>> SOUNDS = new java.util.HashMap<>();
    static {
        for (String id : new String[]{"generator_operating", "windmill_operating", "reactor_operating"}) SOUNDS.put(id, SOUND_EVENTS.register(id, ()->net.minecraft.sounds.SoundEvent.createVariableRangeEvent(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, id))));
    }
    static final DeferredBlock<MonitorBlock> MONITOR = BLOCKS.registerBlock("monitor", MonitorBlock::new, properties());
    static final DeferredBlock<Block> REFINED_IRON_BLOCK = BLOCKS.registerSimpleBlock("refined_iron_block", properties());
    static final DeferredBlock<FluidPipeBlock> PIPE = BLOCKS.registerBlock("pipe", (p)->new FluidPipeBlock(p, false), properties().noOcclusion());
    static final DeferredBlock<FluidPipeBlock> PRESSURE_PIPE = BLOCKS.registerBlock("pressure_pipe", (p)->new FluidPipeBlock(p, true), properties().noOcclusion());
    static final DeferredBlock<PoweredLampBlock> LAMP = BLOCKS.registerBlock("luminator", (p)->new PoweredLampBlock(p, false), properties().lightLevel((s)->s.getValue(PoweredLampBlock.LIGHT)));
    static final DeferredBlock<PoweredLampBlock> ADJUSTABLE_LAMP = BLOCKS.registerBlock("adjustable_luminator", (p)->new PoweredLampBlock(p, true), properties().lightLevel((s)->s.getValue(PoweredLampBlock.LIGHT)));
    static final Supplier<BlockEntityType<MonitorEntity>> MONITOR_ENTITY = ENTITIES.register("monitor", ()->BlockEntityType.Builder.of(MonitorEntity::new, MONITOR.get()).build(null));
    static final Supplier<BlockEntityType<FluidPipeEntity>> PIPE_ENTITY = ENTITIES.register("fluid_pipe", ()->BlockEntityType.Builder.of(FluidPipeEntity::new, PIPE.get(), PRESSURE_PIPE.get()).build(null));
    static final Supplier<BlockEntityType<PoweredLampEntity>> LAMP_ENTITY = ENTITIES.register("luminator", ()->BlockEntityType.Builder.of(PoweredLampEntity::new, LAMP.get(), ADJUSTABLE_LAMP.get()).build(null));
    static final Supplier<MenuType<MonitorMenu>> MONITOR_MENU = MENUS.register("monitor", ()->new MenuType<>(MonitorMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().strength(3).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }

    private NuclearInfrastructure() {
    }

    public static void register(IEventBus bus, DeferredRegister.Items items) {
        BLOCKS.register(bus);
        ENTITIES.register(bus);
        MENUS.register(bus);
        SOUND_EVENTS.register(bus);
        for (var block : new DeferredBlock<?>[]{MONITOR, PIPE, PRESSURE_PIPE, LAMP, ADJUSTABLE_LAMP, REFINED_IRON_BLOCK}) items.registerSimpleBlockItem(block);
        for (String item : new String[]{"frequency_transmitter", "monitor_card", "fluid_import_upgrade_simple"}) items.registerSimpleItem(item);
        bus.addListener(NuclearInfrastructure::capabilities);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(WindWorldData::tick);
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PIPE_ENTITY.get(), (pipe,side)->pipe.tank);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, MONITOR_ENTITY.get(), (monitor,side)->monitor.cards);
    }
}
