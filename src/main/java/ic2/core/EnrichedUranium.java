package ic2.core;

import ic2.core.item.UraniumFuelRod;
import ic2.core.item.UraniumIsotopicRod;
import ic2.core.item.UraniumType;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class EnrichedUranium {
    private final List<DeferredItem<? extends Item>> items = new ArrayList<>();
    private final Map<UraniumType, DeferredItem<Item>> ingots = new EnumMap<>(UraniumType.class);

    EnrichedUranium(DeferredRegister.Items registry) {
        for (UraniumType type : UraniumType.values()) {
            if (type != UraniumType.STANDARD) {
                registerFamily(registry, type);
            }
        }
    }

    private void registerFamily(DeferredRegister.Items registry, UraniumType type) {
        String family = type.id();
        var ingot = registry.registerSimpleItem("ingot_uranium_enriched_" + family.replace("_", ""));
        ingots.put(type, ingot);
        items.add(ingot);

        var depleted = registry.registerSimpleItem("uranium_rod_near_depleted_" + family);
        var recovered = registry.registerSimpleItem("uranium_rod_re_enriched_" + family);
        items.add(depleted);
        items.add(recovered);
        items.add(registry.registerItem("uranium_rod_isotopic_" + family,
            properties -> new UraniumIsotopicRod(properties, type.durability(), recovered)));
        registerRod(registry, type, depleted, "single", 1);
        registerRod(registry, type, depleted, "dual", 2);
        registerRod(registry, type, depleted, "quad", 4);
    }

    private void registerRod(DeferredRegister.Items registry, UraniumType type, DeferredItem<Item> depleted, String size, int cells) {
        items.add(registry.registerItem("uranium_rod_" + type.id() + "_" + size,
            properties -> new UraniumFuelRod(properties, cells, type, depleted)));
    }

    public Item ingot(UraniumType type) {
        return ingots.get(type).get();
    }

    void addToCreativeTab(BuildCreativeModeTabContentsEvent event) {
        items.forEach(item -> event.accept(item.get()));
    }
}
