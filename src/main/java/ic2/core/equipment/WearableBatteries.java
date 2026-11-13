package ic2.core.equipment;

import ic2.api.item.ElectricItem;
import ic2.core.item.EuCharge;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.registries.DeferredRegister;

final class WearableBatteries {
    private record Definition(String id, ArmorItem.Type type, int capacity, int rate, int packet, Rarity rarity) {}
    static void register(DeferredRegister.Items items) {
        for (var definition : List.of(
            new Definition("batpack", ArmorItem.Type.CHESTPLATE, 60000, 100, 32, Rarity.COMMON),
            new Definition("lappack", ArmorItem.Type.CHESTPLATE, 300000, 250, 128, Rarity.UNCOMMON),
            new Definition("quantum_pack", ArmorItem.Type.CHESTPLATE, 1200000, 1000, 512, Rarity.RARE),
            new Definition("industrial_belt", ArmorItem.Type.LEGGINGS, 30000, 100, 32, Rarity.COMMON))) {
            var material = EquipmentMaterials.simple(definition.id, definition.type, 0);
            items.registerItem(definition.id, p -> new BatteryArmor(material, definition, p), new Item.Properties().stacksTo(1).rarity(definition.rarity));
        }
    }
    private static final class BatteryArmor extends ArmorItem implements ElectricItem {
        private final Definition definition;
        BatteryArmor(Holder<ArmorMaterial> material, Definition definition, Properties properties) { super(material, definition.type, properties); this.definition = definition; }
        @Override public int capacity() { return definition.capacity; }
        @Override public int transferRate() { return definition.rate; }
        @Override public int minimumPacket() { return definition.packet; }
        @Override public int charge(ItemStack stack) { return EuCharge.get(stack, capacity()); }
        @Override public void setCharge(ItemStack stack, int amount) { EuCharge.set(stack, capacity(), amount); }
        @Override public boolean isValidRepairItem(ItemStack stack, ItemStack ingredient) { return false; }
        @Override public boolean isEnchantable(ItemStack stack) { return false; }
        @Override public boolean isBarVisible(ItemStack stack) { return true; }
        @Override public int getBarWidth(ItemStack stack) { return Math.round(13F * charge(stack) / capacity()); }
        @Override public int getBarColor(ItemStack stack) { return 0x42CDEA; }
    }
}
