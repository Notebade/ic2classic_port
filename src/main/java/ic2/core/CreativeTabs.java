package ic2.core;

import java.util.Comparator;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

final class CreativeTabs {
    private static final DeferredRegister<CreativeModeTab> TABS =
        DeferredRegister.create(Registries.CREATIVE_MODE_TAB, IC2.MOD_ID);
    private static final Set<String> TOOLS = Set.of(
        "wrench", "electric_wrench", "cutter", "tree_tap", "electric_tree_tap", "magnet", "dead_magnet");
    private static final Set<String> GENERATORS = Set.of(
        "generator", "geothermal_generator", "thermal_generator", "wind_turbine",
        "wave_generator", "ocean_generator", "steam_turbine");
    private static final Set<String> MACHINES = Set.of(
        "macerator", "stone_macerator", "iron_furnace", "electric_furnace", "electric_blast_furnace",
        "electric_smoker", "induction_furnace", "induction_blast_furnace", "induction_smoker", "sawmill", "alloy_smelter", "extractor", "rare_earth_extractor",
        "electrolyzer", "compressor", "pump", "recycler", "mass_fabricator", "canner", "vacuum_canner");
    private static final Set<String> ENERGY = Set.of(
        "batbox", "mfe", "mfsu", "esu", "isu", "pesu", "glowtronic_crystal", "uesc", "pesd", "quantum_accumulator", "quantum_accumulator_big", "re_battery", "energy_crystal", "lapatron_crystal");
    private static final Set<String> NUCLEAR = Set.of(
        "nuclear_reactor", "steam_reactor", "steam_reactor_chamber", "uranium_enricher",
        "prepared_fuel_cell", "cell_fuel");

    static void register(IEventBus bus) {
        tab("tools", "wrench");
        tab("generators", "generator");
        tab("machines", "macerator");
        tab("energy", "batbox");
        tab("nuclear", "nuclear_reactor");
        tab("materials", "ingot_tin");
        TABS.register(bus);
    }

    private static void tab(String category, String icon) {
        TABS.register(category, () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.ic2." + category))
            .icon(() -> new ItemStack(BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, icon))))
            .displayItems((parameters, output) -> BuiltInRegistries.ITEM.stream()
                .filter(item -> BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(IC2.MOD_ID))
                .filter(item -> category(item).equals(category))
                .sorted(Comparator.comparing(item -> BuiltInRegistries.ITEM.getKey(item).getPath()))
                .forEach(output::accept))
            .build());
    }

    private static String category(Item item) {
        String name = BuiltInRegistries.ITEM.getKey(item).getPath();
        if (TOOLS.contains(name) || name.startsWith("armor_")) {
            return "tools";
        }
        if (GENERATORS.contains(name) || name.startsWith("solar_panel") || name.startsWith("water_mill")) {
            return "generators";
        }
        if (MACHINES.contains(name)) {
            return "machines";
        }
        if (ENERGY.contains(name) || name.endsWith("_cable_item") || name.startsWith("transformer_")) {
            return "energy";
        }
        if (NUCLEAR.contains(name) || name.startsWith("reactor_") || name.startsWith("uranium_rod")
            || name.startsWith("heat_") || name.startsWith("steam_vent") || name.startsWith("electric_vent")
            || name.startsWith("reflector") || name.startsWith("plating") || name.startsWith("condensator")) {
            return "nuclear";
        }
        return "materials";
    }
}
