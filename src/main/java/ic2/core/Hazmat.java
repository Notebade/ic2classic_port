package ic2.core;

import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

final class Hazmat extends ArmorItem {
    private static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, IC2.MOD_ID);
    private static final DeferredHolder<ArmorMaterial, ArmorMaterial> MATERIAL = MATERIALS.register("hazmat", () ->
        new ArmorMaterial(Map.of(Type.HELMET, 1, Type.CHESTPLATE, 3, Type.LEGGINGS, 2, Type.BOOTS, 1, Type.BODY, 0),
            0, SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(IC2.RUBBER.get()),
            List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(IC2.MOD_ID, "hazmat"))), 0, 0));

    private Hazmat(Type type, Item.Properties properties) {
        super(MATERIAL, type, properties);
    }

    static void register(IEventBus bus, DeferredRegister.Items items) {
        MATERIALS.register(bus);
        var helmet = registerPiece(items, "helmet", Type.HELMET);
        var chest = registerPiece(items, "chest", Type.CHESTPLATE);
        var legs = registerPiece(items, "leggings", Type.LEGGINGS);
        var boots = registerPiece(items, "boots", Type.BOOTS);
        bus.addListener((BuildCreativeModeTabContentsEvent event) -> {
            if (event.getTabKey().equals(CreativeModeTabs.COMBAT)) {
                event.accept(helmet.get());
                event.accept(chest.get());
                event.accept(legs.get());
                event.accept(boots.get());
            }
        });
    }

    private static DeferredItem<Hazmat> registerPiece(DeferredRegister.Items items, String name, Type type) {
        return items.registerItem("armor_hazmat_" + name, properties -> new Hazmat(type, properties),
            new Item.Properties().durability(64));
    }

    static boolean protects(LivingEntity entity) {
        return wears(entity, EquipmentSlot.HEAD, Type.HELMET)
            && wears(entity, EquipmentSlot.CHEST, Type.CHESTPLATE)
            && wears(entity, EquipmentSlot.LEGS, Type.LEGGINGS)
            && wears(entity, EquipmentSlot.FEET, Type.BOOTS);
    }

    private static boolean wears(LivingEntity entity, EquipmentSlot slot, Type type) {
        return entity.getItemBySlot(slot).getItem() instanceof Hazmat piece && piece.getType() == type;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.ic2.hazmat_radiation"));
    }
}
