package ic2.core.machine;

import ic2.core.IC2;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ExtraGenerators {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IC2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IC2.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, IC2.MOD_ID);
    static final DeferredBlock<ExtraGeneratorBlock> SOLAR = block("solar_turbine", Type.SOLAR);
    static final DeferredBlock<ExtraGeneratorBlock> SLAG = block("slag_generator", Type.SLAG);
    static final Supplier<BlockEntityType<ExtraGeneratorEntity>> ENTITY = ENTITIES.register("extra_generator",
        () -> BlockEntityType.Builder.of(ExtraGeneratorEntity::new, SOLAR.get(), SLAG.get()).build(null));
    static final Supplier<MenuType<ExtraGeneratorMenu>> SOLAR_MENU = menu("solar_turbine", Type.SOLAR);
    static final Supplier<MenuType<ExtraGeneratorMenu>> SLAG_MENU = menu("slag_generator", Type.SLAG);

    private ExtraGenerators() {}

    public static void register(IEventBus bus, DeferredRegister.Items items) {
        BLOCKS.register(bus); ENTITIES.register(bus); MENUS.register(bus);
        items.registerSimpleBlockItem(SOLAR); items.registerSimpleBlockItem(SLAG);
        bus.addListener(ExtraGenerators::capabilities);
    }

    private static DeferredBlock<ExtraGeneratorBlock> block(String id, Type type) {
        return BLOCKS.registerBlock(id, properties -> new ExtraGeneratorBlock(properties, type),
            BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops());
    }

    private static Supplier<MenuType<ExtraGeneratorMenu>> menu(String id, Type type) {
        return MENUS.register(id, () -> new MenuType<>((window, player) -> new ExtraGeneratorMenu(window, player, type), FeatureFlags.DEFAULT_FLAGS));
    }

    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY.get(), ExtraGeneratorEntity::automation);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ENTITY.get(),
            (generator, side) -> generator.type == Type.SOLAR ? generator.waterHandler : null);
    }

    enum Type {
        SOLAR(32, 32), SLAG(60, 128);
        final int packet;
        final int chargeTier;
        Type(int packet, int chargeTier) { this.packet = packet; this.chargeTier = chargeTier; }
        MenuType<ExtraGeneratorMenu> menu() { return this == SOLAR ? SOLAR_MENU.get() : SLAG_MENU.get(); }
        int packet() { return this == SOLAR ? Math.max(1, (int) (32 * GenerationConfig.SOLAR.get())) : GenerationConfig.SLAG.get(); }
        int maxStored() { return this == SOLAR ? SolarTurbineCycle.NOMINAL_BUFFER + (int) Math.ceil(32 * GenerationConfig.SOLAR.get()) - 1 : SlagGeneratorCycle.CAPACITY; }
    }
}
