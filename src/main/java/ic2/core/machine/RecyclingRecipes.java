package ic2.core.machine;

import ic2.core.IC2;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.common.Tags;

final class RecyclingRecipes {
    static final int BOX_EXTRA_TICKS = 68;
    private static final TagKey<Item> BLACKLIST = TagKey.create(Registries.ITEM,
        ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, "recycler_blacklist"));
    private static final Map<Item, Recipe> SPECIAL = Map.of(
        Items.IRON_HORSE_ARMOR, new Recipe(Items.IRON_INGOT, 5, 1),
        Items.GOLDEN_HORSE_ARMOR, new Recipe(Items.GOLD_INGOT, 5, 1),
        Items.DIAMOND_HORSE_ARMOR, new Recipe(Items.DIAMOND, 5, 1)
    );
    private static final Recipe SCRAP = new Recipe(IC2.SCRAP.get(), 1, 4);
    private static final Recipe SCRAP_METAL = new Recipe(IC2.SCRAP_METAL.get(), 1, 8);
    private static final Recipe SCRAP_BOX = new Recipe(IC2.SCRAP_BOX.get(), 1, 1, 9, BOX_EXTRA_TICKS);

    private RecyclingRecipes() {}

    static Recipe find(ItemStack stack) {
        if (stack.is(IC2.SCRAP.get())) {
            return SCRAP_BOX;
        }
        if (stack.isEmpty() || stack.is(BLACKLIST) || stack.is(IC2.SCRAP_METAL.get())) {
            return null;
        }
        Recipe special = SPECIAL.get(stack.getItem());
        if (special != null) {
            return special;
        }
        return stack.is(Tags.Items.INGOTS) && !stack.is(Tags.Items.BRICKS) ? SCRAP_METAL : SCRAP;
    }

    record Recipe(Item output, int count, int chanceDenominator, int inputCount, int extraTicks) {
        Recipe(Item output, int count, int chanceDenominator) {
            this(output, count, chanceDenominator, 1, 0);
        }
    }
}
