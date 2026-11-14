package ic2.core.equipment;

import java.util.EnumMap;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

final class EquipmentMaterials {
    private static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, "ic2");
    private EquipmentMaterials() {}
    static Holder<ArmorMaterial> simple(String id, ArmorItem.Type slot, int defense) {
        var values = new EnumMap<ArmorItem.Type, Integer>(ArmorItem.Type.class);
        for (var type : ArmorItem.Type.values()) values.put(type, 0);
        values.put(slot, defense);
        return MATERIALS.register(id, () -> new ArmorMaterial(values, 0, SoundEvents.ARMOR_EQUIP_IRON, () -> Ingredient.EMPTY,
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath("ic2", id))), 0, 0));
    }
    static Holder<ArmorMaterial> electricSuit(String id, boolean dyeable) {
        var asset = ResourceLocation.fromNamespaceAndPath("ic2", id);
        return MATERIALS.register(id, () -> {
            var base = net.minecraft.world.item.ArmorMaterials.DIAMOND.value();
            var layers = dyeable ? List.of(new ArmorMaterial.Layer(asset, "", true), new ArmorMaterial.Layer(asset, "_overlay", false))
                : List.of(new ArmorMaterial.Layer(asset));
            return new ArmorMaterial(base.defense(), base.enchantmentValue(), base.equipSound(), () -> Ingredient.EMPTY,
                layers, base.toughness(), base.knockbackResistance());
        });
    }
    static void register(IEventBus bus) { MATERIALS.register(bus); }
}
