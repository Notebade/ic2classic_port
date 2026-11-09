package ic2.core;

import ic2.core.item.ReactorHeatItem;
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
    }

    private void registerVent(DeferredRegister.Items registry, String name, int selfCooling, int hullCooling, boolean electric) {
        int capacity = electric ? 10_000 : 1_000;
        items.add(registry.registerItem(name,
            properties -> new ReactorHeatItem.Vent(properties, capacity, selfCooling, hullCooling, electric)));
    }

    void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        items.forEach(item -> event.accept(item.get()));
    }
}
