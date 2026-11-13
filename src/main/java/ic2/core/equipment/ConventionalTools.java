package ic2.core.equipment;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.HoeItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.ShovelItem;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.registries.DeferredRegister;

final class ConventionalTools {
    static void register(DeferredRegister.Items items) {
        for (Material material : Material.values()) {
            String suffix = material.name().toLowerCase(java.util.Locale.ROOT);
            items.registerItem("tool_axe_" + suffix, p -> new AxeItem(material, p.attributes(AxeItem.createAttributes(material, 6, -3.1F))));
            items.registerItem("tool_pickaxe_" + suffix, p -> new PickaxeItem(material, p.attributes(PickaxeItem.createAttributes(material, 1, -2.8F))));
            items.registerItem("tool_shovel_" + suffix, p -> new ShovelItem(material, p.attributes(ShovelItem.createAttributes(material, 1.5F, -3))));
            items.registerItem("tool_hoe_" + suffix, p -> new HoeItem(material, p.attributes(HoeItem.createAttributes(material, 2, -1))));
            items.registerItem("tool_sword_" + suffix, p -> new SwordItem(material, p.attributes(SwordItem.createAttributes(material, 3, -2.4F))));
        }
    }
    private enum Material implements Tier {
        BRONZE(350, 6), ALUMINIUM(700, 10);
        private final int durability;
        private final float speed;
        Material(int durability, float speed) { this.durability = durability; this.speed = speed; }
        @Override public int getUses() { return durability; }
        @Override public float getSpeed() { return speed; }
        @Override public float getAttackDamageBonus() { return 2; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_IRON_TOOL; }
        @Override public int getEnchantmentValue() { return 13; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2", "ingot_" + name().toLowerCase(java.util.Locale.ROOT)))); }
    }
}
