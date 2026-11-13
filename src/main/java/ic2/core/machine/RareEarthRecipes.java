package ic2.core.machine;

import ic2.core.IC2;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

final class RareEarthRecipes {
    // The original yields are exact multiples of 1/64, including netherrack's 7.8125.
    static final int UNITS_PER_OUTPUT = RareEarthProgress.UNITS_PER_OUTPUT;
    private static final Entry STONE = new Entry(Material.RARE_EARTH, 200);
    private static final Entry IGNEOUS = new Entry(Material.RARE_EARTH, 4000);
    private static final Entry RICH = new Entry(Material.RARE_EARTH, 12800);
    private static final Entry CLAY = new Entry(Material.ALUMINIUM, 32000);
    private static final Map<Item, Entry> INPUTS = Map.ofEntries(
        Map.entry(Items.STONE, STONE),
        Map.entry(Items.DEEPSLATE, STONE),
        Map.entry(Items.GRANITE, IGNEOUS),
        Map.entry(Items.DIORITE, IGNEOUS),
        Map.entry(Items.ANDESITE, IGNEOUS),
        Map.entry(Items.TUFF, IGNEOUS),
        Map.entry(Items.NETHERRACK, new Entry(Material.RARE_EARTH, 500)),
        Map.entry(Items.MYCELIUM, new Entry(Material.RARE_EARTH, 6400)),
        Map.entry(Items.OBSIDIAN, RICH),
        Map.entry(Items.SOUL_SAND, RICH),
        Map.entry(Items.SOUL_SOIL, RICH),
        Map.entry(Items.CLAY_BALL, CLAY),
        Map.entry(Items.DIRT, new Entry(Material.ALUMINIUM, 6400))
    );

    private RareEarthRecipes() {}

    static Entry find(ItemStack input) {
        return input.is(ItemTags.TERRACOTTA) ? CLAY : INPUTS.get(input.getItem());
    }

    enum Material {
        RARE_EARTH("rare_earth", IC2.RARE_EARTH_DUST),
        ALUMINIUM("aluminium", IC2.ALUMINIUM_DUST);

        final String id;
        private final Supplier<? extends Item> dust;

        Material(String id, Supplier<? extends Item> dust) {
            this.id = id;
            this.dust = dust;
        }

        static Material byId(String id) {
            return switch (id) {
                case "rare_earth" -> RARE_EARTH;
                case "aluminium" -> ALUMINIUM;
                default -> null;
            };
        }

        ItemStack output() {
            return new ItemStack(dust.get(), 2);
        }
    }

    record Entry(Material material, int units) {}
}
