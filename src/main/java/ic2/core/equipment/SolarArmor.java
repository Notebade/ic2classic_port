package ic2.core.equipment;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.registries.DeferredRegister;

final class SolarArmor extends ArmorItem implements ic2.api.item.ArmorTickItem {
    private final int production;
    private SolarArmor(Holder<ArmorMaterial> material, Properties properties, int production) { super(material, Type.HELMET, properties); this.production = production; }
    static void register(DeferredRegister.Items items) {
        for (boolean advanced : new boolean[]{false, true}) {
            String id = advanced ? "advanced_solar_helmet" : "solar_helmet";
            var material = EquipmentMaterials.simple(id, Type.HELMET, 1);
            items.registerItem(id, p -> new SolarArmor(material, p, advanced ? 5 : 1), new Properties().stacksTo(1).rarity(advanced ? Rarity.UNCOMMON : Rarity.COMMON));
        }
    }
    @Override public void tickArmor(ItemStack stack, net.minecraft.world.entity.player.Player player, EquipmentSlot slot) {
        if (slot == EquipmentSlot.HEAD && sunlight(player.level(), player.blockPosition())) ic2.api.item.ElectricEquipment.offerToArmor(player, production);
    }
    static boolean sunlight(Level level, net.minecraft.core.BlockPos pos) {
        return level.dimensionType().hasSkyLight() && level.isDay() && level.canSeeSkyFromBelowWater(pos)
            && (!level.getBiome(pos).value().hasPrecipitation() || !level.isRaining() && !level.isThundering());
    }
    @Override public boolean isEnchantable(ItemStack stack) { return false; }
}
