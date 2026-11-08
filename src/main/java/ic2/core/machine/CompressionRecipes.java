package ic2.core.machine;

import ic2.core.IC2;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class CompressionRecipes {
    private static final Map<Item, Recipe> RECIPES = Map.of(
        Items.SAND, new Recipe(1, Items.SANDSTONE),
        Items.RED_SAND, new Recipe(1, Items.RED_SANDSTONE),
        Items.NETHERRACK, new Recipe(3, Items.NETHER_BRICKS),
        Items.SNOWBALL, new Recipe(1, Items.ICE),
        Items.ICE, new Recipe(3, Items.PACKED_ICE),
        Items.PACKED_ICE, new Recipe(3, Items.BLUE_ICE),
        Items.BLAZE_POWDER, new Recipe(5, Items.BLAZE_ROD),
        Items.COPPER_INGOT, new Recipe(8, IC2.DENSE_COPPER_PLATE.get()),
        Machines.SOLAR_PANEL.get().asItem(), new Recipe(1, Machines.COMPRESSED_SOLAR_PANEL.get().asItem())
    );

    private CompressionRecipes() {}

    static Recipe find(ItemStack input) {
        return RECIPES.get(input.getItem());
    }

    record Recipe(int inputCount, Item output) {}
}
