package ic2.core.machine;

import static java.util.Map.entry;

import ic2.core.IC2;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class CompressionRecipes {
    private static final Map<Item, Recipe> RECIPES = Map.ofEntries(
        entry(IC2.SCRAP_METAL.get(), new Recipe(8, IC2.SCRAP_METAL_CHUNK.get())),
        entry(Items.SAND, new Recipe(1, Items.SANDSTONE)),
        entry(Items.RED_SAND, new Recipe(1, Items.RED_SANDSTONE)),
        entry(Items.NETHERRACK, new Recipe(3, Items.NETHER_BRICKS)),
        entry(Items.SNOWBALL, new Recipe(1, Items.ICE)),
        entry(Items.ICE, new Recipe(3, Items.PACKED_ICE)),
        entry(Items.PACKED_ICE, new Recipe(3, Items.BLUE_ICE)),
        entry(Items.BLAZE_POWDER, new Recipe(5, Items.BLAZE_ROD)),
        entry(Items.COPPER_INGOT, new Recipe(8, IC2.DENSE_COPPER_PLATE.get())),
        entry(Machines.SOLAR_PANEL.get().asItem(), new Recipe(1, Machines.COMPRESSED_SOLAR_PANEL.get().asItem())),
        entry(Machines.LV_SOLAR_PANEL.get().asItem(), new Recipe(1, Machines.COMPRESSED_LV_SOLAR_PANEL.get().asItem())),
        entry(Machines.MV_SOLAR_PANEL.get().asItem(), new Recipe(1, Machines.COMPRESSED_MV_SOLAR_PANEL.get().asItem())),
        entry(Machines.HV_SOLAR_PANEL.get().asItem(), new Recipe(1, Machines.COMPRESSED_HV_SOLAR_PANEL.get().asItem())),
        entry(IC2.ADVANCED_ALLOY.get(), new Recipe(1, IC2.ADVANCED_ALLOY_PLATE.get())),
        entry(IC2.CARBON_MESH.get(), new Recipe(1, IC2.CARBON_PLATE.get())),
        entry(IC2.CELL_EMPTY.get(), new Recipe(1, IC2.CELL_AIR.get())),
        entry(IC2.RARE_EARTH_CHUNK.get(), new Recipe(1, IC2.DEAD_MAGNET.get()))
    );

    private CompressionRecipes() {}

    static Recipe find(ItemStack input) {
        return RECIPES.get(input.getItem());
    }

    record Recipe(int inputCount, Item output) {}
}
