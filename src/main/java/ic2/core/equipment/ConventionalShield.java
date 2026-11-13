package ic2.core.equipment;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.neoforged.neoforge.registries.DeferredRegister;

final class ConventionalShield extends ShieldItem {
    private final String repair;

    private ConventionalShield(Properties properties, String repair) {
        super(properties);
        this.repair = repair;
    }

    static void register(DeferredRegister.Items items) {
        register(items, "bronze", 1000, "ingot_bronze");
        register(items, "aluminium", 2000, "ingot_aluminium");
        register(items, "composite", 3000, "ingot_advanced_alloy");
    }

    private static void register(DeferredRegister.Items items, String material, int durability, String repair) {
        items.registerItem(material + "_shield", p -> new ConventionalShield(p, repair), new Properties().durability(durability));
    }

    @Override
    public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) {
        return ingredient.is(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2", repair)));
    }
}
