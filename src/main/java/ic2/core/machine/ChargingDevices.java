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
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ChargingDevices {
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(IC2.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> ENTITIES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, IC2.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, IC2.MOD_ID);
    static final Map<Kind, DeferredBlock<ChargingBlock>> BLOCK = new EnumMap<>(Kind.class);
    static final Map<Kind, Supplier<MenuType<ChargingMenu>>> MENU = new EnumMap<>(Kind.class);
    static {
        for (Kind kind : Kind.values()) {
            BLOCK.put(kind, BLOCKS.registerBlock(kind.id, p -> kind.pad() ? new ChargingPadBlock(p, kind) : kind.family == Family.BENCH ? new ChargingBenchBlock(p, kind) : new ChargingBlock(p, kind),
                BlockBehaviour.Properties.of().strength(3).sound(SoundType.METAL).requiresCorrectToolForDrops()));
            MENU.put(kind, MENUS.register(kind.id, () -> new MenuType<>((id, player) -> new ChargingMenu(id, player, kind), FeatureFlags.DEFAULT_FLAGS)));
        }
    }
    static final Supplier<BlockEntityType<ChargingEntity>> ENTITY = ENTITIES.register("charging_device", () ->
        BlockEntityType.Builder.of(ChargingEntity::new, BLOCK.values().stream().map(Supplier::get).toArray(Block[]::new)).build(null));
    private ChargingDevices() {}
    public static void register(IEventBus bus, DeferredRegister.Items items) {
        PadUpgradeItem.register(items);
        MachineUpgradeItem.register(items);
        items.registerItem("upgrade_simple_import", p -> new TransportUpgradeItem(p, TransportUpgradeItem.Kind.ITEM_IMPORT));
        items.registerItem("upgrade_simple_export", p -> new TransportUpgradeItem(p, TransportUpgradeItem.Kind.ITEM_EXPORT));
        BLOCKS.register(bus); ENTITIES.register(bus); MENUS.register(bus); BLOCK.values().forEach(items::registerSimpleBlockItem);
        items.registerSimpleItem("tfbp"); items.registerSimpleItem("tfbp_irrigation");
        bus.addListener(ChargingDevices::capabilities);
    }
    private static void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, ENTITY.get(), (device, side) -> device.kind.inventoryGrid() ? device.automation : null);
    }
    enum Family { BENCH, STATION, PAD, MASTER, SLAVE }
    enum Kind {
        BENCH_LV("charging_bench_lv", Family.BENCH, 40000, 32, 0),
        BENCH_MV("charging_bench_mv", Family.BENCH, 600000, 128, 0),
        BENCH_HV("charging_bench_hv", Family.BENCH, 10000000, 512, 0),
        BENCH_EV("charging_bench_ev", Family.BENCH, 75000000, 2048, 0),
        BENCH_IV("charging_bench_iv", Family.BENCH, 500000000, 8192, 0),
        STATION_LV("battery_station_lv", Family.STATION, 40000, 32, 0),
        STATION_MV("battery_station_mv", Family.STATION, 600000, 128, 0),
        STATION_HV("battery_station_hv", Family.STATION, 75000000, 512, 0),
        STATION_EV("battery_station_ev", Family.STATION, 75000000, 2048, 0),
        STATION_IV("battery_station_iv", Family.STATION, 500000000, 8192, 0),
        PAD_LV("chargepad_lv", Family.PAD, 40000, 32, 0),
        PAD_MV("chargepad_mv", Family.PAD, 600000, 128, .5),
        PAD_HV("chargepad_hv", Family.PAD, 1000000, 512, .8),
        PAD_FISSION("chargepad_fission", Family.PAD, 15000000, 2048, 1),
        MASTER("redirector_master", Family.MASTER, 5, 32768, 0),
        SLAVE("redirector_slave", Family.SLAVE, 64 * 32768 + 31, 32768, 0);
        final String id; final Family family; final int capacity, packet; final double range;
        Kind(String id, Family family, int capacity, int packet, double range) { this.id = id; this.family = family; this.capacity = capacity; this.packet = packet; this.range = range; }
        boolean pad() { return family == Family.PAD; }
        boolean inventoryGrid() { return family == Family.BENCH || family == Family.STATION; }
        boolean node() { return family == Family.MASTER || family == Family.SLAVE; }
        int tier() { return this == PAD_LV ? 1 : this == PAD_MV ? 2 : this == PAD_HV ? 3 : 4; }
        int projectionSlots() { return pad() && this != PAD_LV ? 2 : 0; }
        int machineUpgradeSlots() { return !pad() ? 0 : tier() == 1 ? 1 : tier() == 2 ? 2 : 3; }
        int slots() { return inventoryGrid() ? 17 : pad() ? 1 + projectionSlots() + machineUpgradeSlots() : 0; }
        ChargingBlock block() { return BLOCK.get(this).get(); }
        MenuType<ChargingMenu> menu() { return MENU.get(this).get(); }
    }
}
