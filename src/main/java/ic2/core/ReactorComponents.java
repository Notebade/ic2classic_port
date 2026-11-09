package ic2.core;

import ic2.core.item.ReactorHeatItem;
import ic2.core.item.ReactorPlatingItem;
import ic2.core.item.ReactorReflectorItem;
import ic2.core.machine.HeatPumpItem;
import ic2.core.machine.ReactorCardItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

final class ReactorComponents {
    private final List<DeferredItem<? extends Item>> items = new ArrayList<>();

    ReactorComponents(DeferredRegister.Items registry) {
        items.add(registry.registerItem("heat_storage_single", properties -> new ReactorHeatItem(properties, 10_000)));
        items.add(registry.registerItem("heat_storage_triple", properties -> new ReactorHeatItem(properties, 30_000)));
        items.add(registry.registerItem("heat_storage_six", properties -> new ReactorHeatItem(properties, 60_000)));
        registerVent(registry, "heat_vent", 6, 0, false);
        registerVent(registry, "heat_vent_core", 5, 5, false);
        registerVent(registry, "heat_vent_overclocked", 20, 36, false);
        registerVent(registry, "heat_vent_advanced", 12, 0, false);
        registerVent(registry, "electric_vent", 12, 0, true);
        registerVent(registry, "electric_vent_core", 10, 10, true);
        registerVent(registry, "electric_vent_overclocked", 40, 72, true);
        registerVent(registry, "electric_vent_advanced", 24, 0, true);
        registerSteamVent(registry, "steam_vent", 12, 0);
        registerSteamVent(registry, "steam_vent_core", 10, 10);
        registerSteamVent(registry, "steam_vent_overclocked", 40, 72);
        registerSteamVent(registry, "steam_vent_advanced", 24, 0);
        registerExchanger(registry, "heat_exchanger", 2_500, 12, 4, false);
        registerExchanger(registry, "heat_exchanger_core", 5_000, 0, 72, false);
        registerExchanger(registry, "heat_exchanger_spread", 5_000, 36, 0, false);
        registerExchanger(registry, "heat_exchanger_advanced", 10_000, 24, 8, false);
        registerExchanger(registry, "heat_balancer", 2_500, 18, 12, true);
        registerExchanger(registry, "heat_balancer_core", 5_000, 0, 108, true);
        registerExchanger(registry, "heat_balancer_spread", 5_000, 54, 0, true);
        registerExchanger(registry, "heat_balancer_advanced", 10_000, 36, 24, true);
        items.add(registry.registerItem("heat_spreader", ReactorHeatItem.Spreader::new));
        items.add(registry.registerItem("heat_pump", HeatPumpItem::new));
        items.add(registry.registerItem("reactor_card", ReactorCardItem::new));
        items.add(registry.registerItem("condensator", properties -> new ReactorHeatItem.Condensator(properties, 20_000)));
        items.add(registry.registerItem("condensator_lap", properties -> new ReactorHeatItem.Condensator(properties, 100_000)));
        items.add(registry.registerItem("plating", properties -> new ReactorPlatingItem(properties, 1_000, 0.95F)));
        items.add(registry.registerItem("plating_heat_capacity", properties -> new ReactorPlatingItem(properties, 2_000, 0.99F)));
        items.add(registry.registerItem("plating_containment", properties -> new ReactorPlatingItem(properties, 500, 0.90F)));
        items.add(registry.registerSimpleItem("ore_iridium"));
        items.add(registry.registerSimpleItem("plate_iridium"));
        items.add(registry.registerItem("reflector_iridium", properties -> new ReactorReflectorItem(properties, 320_000, true)));
    }

    private void registerVent(DeferredRegister.Items registry, String name, int selfCooling, int hullCooling, boolean electric) {
        int capacity = electric ? 10_000 : 1_000;
        items.add(registry.registerItem(name,
            properties -> new ReactorHeatItem.Vent(properties, capacity, selfCooling, hullCooling, electric)));
    }

    private void registerSteamVent(DeferredRegister.Items registry, String name, int selfCooling, int hullCooling) {
        items.add(registry.registerItem(name, properties -> new ReactorHeatItem.SteamVent(properties, selfCooling, hullCooling)));
    }

    private void registerExchanger(DeferredRegister.Items registry, String name, int capacity, int componentTransfer,
                                   int hullTransfer, boolean selective) {
        items.add(registry.registerItem(name,
            properties -> new ReactorHeatItem.Exchanger(properties, capacity, componentTransfer, hullTransfer, selective)));
    }

    void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        items.forEach(item -> event.accept(item.get()));
    }
}
