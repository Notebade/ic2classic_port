package ic2.core.machine;

import ic2.core.IC2;
import java.util.Map;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class Electrolysis {
    private static final Recipe WATER = new Recipe(IC2.CELL_WATER.get(), IC2.ELECTROLYZED_WATER.get(), 20_000);
    private static final Map<Item, Recipe> CHARGE = Map.of(
        WATER.input(), WATER,
        IC2.DEAD_MAGNET.get(), new Recipe(IC2.DEAD_MAGNET.get(), IC2.MAGNET.get(), 20_000)
    );
    private static final Map<Item, Recipe> DISCHARGE = Map.of(
        WATER.output(), WATER,
        Items.ANDESITE, new Recipe(Items.QUARTZ, Items.ANDESITE, 1_000),
        Items.DIORITE, new Recipe(Items.QUARTZ, Items.DIORITE, 1_000),
        Items.GRANITE, new Recipe(Items.QUARTZ, Items.GRANITE, 1_000)
    );

    private Electrolysis() {}

    static Recipe charge(ItemStack stack) {
        return CHARGE.get(stack.getItem());
    }

    static Recipe discharge(ItemStack stack) {
        return DISCHARGE.get(stack.getItem());
    }

    record Recipe(Item input, Item output, int energy) {}
}
