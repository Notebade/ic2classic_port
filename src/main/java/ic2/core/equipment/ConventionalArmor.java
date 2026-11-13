package ic2.core.equipment;

import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

final class ConventionalArmor {
    private static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, "ic2");

    static void register(IEventBus bus, DeferredRegister.Items items) {
        registerSet(items, "bronze", 15, 3, 6, 8, 9, 0, "ingot_bronze");
        registerSet(items, "composite", 50, 4, 7, 9, 12, 4, "ingot_advanced_alloy");
        MATERIALS.register(bus);
    }

    private static void registerSet(DeferredRegister.Items items, String name, int durability, int ends, int legs, int chest, int enchantability, float toughness, String repair) {
        var material = MATERIALS.register(name, () -> new ArmorMaterial(
            Map.of(ArmorItem.Type.HELMET, ends, ArmorItem.Type.CHESTPLATE, chest, ArmorItem.Type.LEGGINGS, legs, ArmorItem.Type.BOOTS, ends, ArmorItem.Type.BODY, 0),
            enchantability, SoundEvents.ARMOR_EQUIP_IRON,
            () -> Ingredient.of(BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2", repair))),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("ic2", name))), toughness, 0));
        for (var type : List.of(ArmorItem.Type.HELMET, ArmorItem.Type.CHESTPLATE, ArmorItem.Type.LEGGINGS, ArmorItem.Type.BOOTS)) {
            String piece = type == ArmorItem.Type.CHESTPLATE ? "chest" : type.getName();
            items.registerItem("armor_" + name + "_" + piece,
                p -> new ArmorItem(material, type, p), new Item.Properties().durability(type.getDurability(durability)));
        }
    }
}
