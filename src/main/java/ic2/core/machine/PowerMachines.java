package ic2.core.machine;

import ic2.core.IC2;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class PowerMachines {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IC2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IC2.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, IC2.MOD_ID);
    static final Map<Kind, DeferredBlock<PowerMachineBlock>> MACHINES = new EnumMap<>(Kind.class);
    static final Map<Kind, Supplier<MenuType<PowerMachineMenu>>> SCREENS = new EnumMap<>(Kind.class);
    static {
        for (Kind kind : Kind.values()) {
            MACHINES.put(kind, BLOCKS.registerBlock(kind.id, p -> new PowerMachineBlock(p, kind),
                kind == Kind.WHISKY_BARREL ? properties().strength(2).sound(SoundType.WOOD) : properties()));
            SCREENS.put(kind, MENUS.register(kind.id, () -> new MenuType<>((id, inv) -> new PowerMachineMenu(id, inv, kind), FeatureFlags.DEFAULT_FLAGS)));
        }
    }
    static final DeferredBlock<Block> REINFORCED_STONE = BLOCKS.registerSimpleBlock("reinforced_stone", properties().strength(80, 1800));
    static final DeferredBlock<Block> IRIDIUM_STONE = BLOCKS.registerSimpleBlock("iridium_stone", properties().strength(100, 3000));
    static final DeferredBlock<net.minecraft.world.level.block.IronBarsBlock> IRON_FENCE = BLOCKS.registerBlock("iron_fence",
        net.minecraft.world.level.block.IronBarsBlock::new, properties().noOcclusion());
    static final DeferredBlock<PowerPartBlock> FUSION_VALVE = part("fusion_reactor_valve");
    static final DeferredBlock<PowerPartBlock> VALVE = part("valve");
    static final DeferredBlock<PowerPartBlock> TUNNEL_CASING = part("steam_tunnel_structure");
    static final DeferredBlock<PowerPartBlock> TUNNEL_VALVE = part("steam_tunnel_valve");
    static final DeferredBlock<PowerPartBlock> BOILER_CASING = part("fuel_boiler_structure");
    static final DeferredBlock<PowerTankBlock> MACHINE_TANK = BLOCKS.registerBlock("machine_tank", PowerTankBlock::new, properties());
    static final DeferredBlock<PowerTankBlock> PERSONAL_TANK = BLOCKS.registerBlock("personal_tank", PowerTankBlock::new, properties());
    static final Supplier<BlockEntityType<PowerTankEntity>> TANK_ENTITY = ENTITIES.register("power_tank", () ->
        BlockEntityType.Builder.of(PowerTankEntity::new, MACHINE_TANK.get(), PERSONAL_TANK.get()).build(null));
    static final DeferredBlock<EnergyCableBlock> SUPER_CABLE = cable("super_cable", 1, 8192, 3);
    static final DeferredBlock<EnergyCableBlock> PLASMA_CABLE = cable("plasma_cable", 1200, 32768, 6);
    static final Supplier<BlockEntityType<PowerMachineEntity>> ENTITY = ENTITIES.register("power_machine", () ->
        BlockEntityType.Builder.of(PowerMachineEntity::new, MACHINES.values().stream().map(Supplier::get).toArray(Block[]::new)).build(null));
    static final Supplier<BlockEntityType<PowerPartEntity>> PART_ENTITY = ENTITIES.register("power_part", () ->
        BlockEntityType.Builder.of(PowerPartEntity::new, VALVE.get(), FUSION_VALVE.get(), TUNNEL_CASING.get(), TUNNEL_VALVE.get(), BOILER_CASING.get()).build(null));
    public static DeferredItem<Item> PLASMA_CELL;
    public static DeferredItem<Item> PLASMA_CORE;
    public static DeferredItem<Item> UNCOMPRESSED_SUPER;
    public static DeferredItem<Item> POLISHED_GOLD;

    private PowerMachines() {}
    static BlockBehaviour.Properties properties() {
        return BlockBehaviour.Properties.of().strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops();
    }
    private static DeferredBlock<PowerPartBlock> part(String id) { return BLOCKS.registerBlock(id, PowerPartBlock::new, properties()); }
    private static DeferredBlock<EnergyCableBlock> cable(String id, int loss, int packet, int radius) {
        return BLOCKS.registerBlock(id, p -> new EnergyCableBlock(p, loss, packet, radius), properties().noOcclusion());
    }
    public static void register(IEventBus bus, DeferredRegister.Items items) {
        BLOCKS.register(bus); ENTITIES.register(bus); MENUS.register(bus);
        MACHINES.values().forEach(items::registerSimpleBlockItem);
        items.registerSimpleBlockItem(VALVE);
        for (var block : new DeferredBlock<?>[]{REINFORCED_STONE, IRIDIUM_STONE, FUSION_VALVE, TUNNEL_CASING, TUNNEL_VALVE, BOILER_CASING, MACHINE_TANK, PERSONAL_TANK, IRON_FENCE}) items.registerSimpleBlockItem(block);
        items.registerSimpleBlockItem("super_cable_item", SUPER_CABLE);
        items.registerSimpleBlockItem("plasma_cable_item", PLASMA_CABLE);
        PLASMA_CELL = items.registerSimpleItem("cell_plasma");
        PLASMA_CORE = items.registerSimpleItem("plasma_core");
        UNCOMPRESSED_SUPER = items.registerSimpleItem("uncompressed_super_cable");
        POLISHED_GOLD = items.registerSimpleItem("ingot_polished_gold");
        BoilerFuel.register(items);
        WindRotor.register(items);
        GeneratorFluids.register(bus, items);
        ChemistryMaterials.register(bus, items);
        StorageInfrastructure.register(bus, items);
        bus.addListener(PowerMachines::capabilities);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY.get(), PowerMachineEntity::automation);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, ENTITY.get(), (entity, side) -> entity.fluidHandler());
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, ENTITY.get(), PowerMachineEntity::feHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, TANK_ENTITY.get(), (tank, side) -> tank.tank);
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, PART_ENTITY.get(), (part, side) -> part.items(side));
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, PART_ENTITY.get(), (part, side) -> part.fluids());
    }
    enum Kind {
        TRANSFORMER("adjustable_transformer", 32768, 1048576, 0),
        FLUX_LV("lv_flux_generator", 32, 160000, 1),
        FLUX_MV("mv_flux_generator", 128, 640000, 1),
        FLUX_HV("hv_flux_generator", 512, 3200000, 1),
        PLASMAFIER("plasmafier", 2048, PlasmaCycle.CAPACITY, 3),
        FUSION("fusion_reactor", 0, 0, 7),
        TUNNEL("steam_tunnel", 0, 10000, 0),
        BOILER("fuel_boiler", 0, 0, 6),
        WINDMILL("windmill", 0, 4000, 1),
        LIQUID_FUEL("liquid_fuel_generator", 0, 10000, 3),
        GASIFIER("wood_gassifier", 0, 0, 5),
        ELECTRIC_GASIFIER("electric_wood_gassifier", 32, 500, 5),
        REFINERY("refinery", 128, 5624, 5),
        WHISKY_BARREL("barrel", 0, 0, 5);
        final String id;
        final int input, capacity, slots;
        Kind(String id, int input, int capacity, int slots) { this.id = id; this.input = input; this.capacity = capacity; this.slots = slots; }
        boolean flux() { return this == FLUX_LV || this == FLUX_MV || this == FLUX_HV; }
        boolean multiblock() { return this == FUSION || this == TUNNEL || this == BOILER; }
        boolean chemical() { return this == GASIFIER || this == ELECTRIC_GASIFIER || this == REFINERY || this == WHISKY_BARREL; }
        boolean outputSlot(int slot) {
            return switch (this) {
                case PLASMAFIER, LIQUID_FUEL -> slot == 2;
                case BOILER -> slot == 1 || slot == 3;
                case GASIFIER, ELECTRIC_GASIFIER -> slot == 2 || slot == 4;
                case REFINERY -> slot >= 2;
                case WHISKY_BARREL -> slot >= 3;
                default -> false;
            };
        }
        PowerMachineBlock block() { return MACHINES.get(this).get(); }
        MenuType<PowerMachineMenu> menu() { return SCREENS.get(this).get(); }
    }
}
