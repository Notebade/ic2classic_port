package ic2.core.equipment;

import ic2.api.item.*;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemContainerContents;

final class ArmorModules {
    static ArmorConfiguration configuration(ItemStack armor) { return armor.getOrDefault(ArmorConfiguration.TYPE.get(), ArmorConfiguration.EMPTY); }
    static List<InstalledArmorModule> read(ItemStack armor) {
        var categories = configuration(armor).categories();
        var contents = armor.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY);
        var result = new ArrayList<InstalledArmorModule>();
        for (int i = 0; i < Math.min(categories.size(), contents.getSlots()); i++) {
            var stack = contents.getStackInSlot(i);
            if (!stack.isEmpty()) result.add(new InstalledArmorModule(categories.get(i), stack));
        }
        return List.copyOf(result);
    }
    static ResourceLocation type(ItemStack module) {
        if (module.getItem() instanceof ArmorBattery battery && battery.armorTransferRate(module) > 0) return ArmorModule.BATTERY;
        return module.getItem() instanceof ArmorModule item ? item.category(module) : null;
    }
    static ArmorEnergyModifier energy(ItemStack module, ItemStack armor) {
        if (module.getItem() instanceof ArmorBattery battery && battery.armorTransferRate(module) > 0) {
            int tier = 1; long packet = 32;
            while (packet < battery.minimumPacket(module) && tier < 14) { tier++; packet *= 4; }
            return new ArmorEnergyModifier(battery.capacity(module), tier, battery.armorTransferRate(module), false);
        }
        return module.getItem() instanceof ArmorModule item ? item.energyModifier(module, armor) : ArmorEnergyModifier.NONE;
    }
    static boolean shares(ItemStack module, ItemStack armor) {
        return module.getItem() instanceof ArmorBattery battery && battery.armorTransferRate(module) > 0
            || module.getItem() instanceof ArmorModule item && item.sharesArmorEnergy(module, armor);
    }
    static ArmorConfiguration derive(ItemStack armor, List<InstalledArmorModule> entries, boolean enhanced) {
        long capacity = 0, rate = 0; int tier = 0; boolean provider = false;
        double storageMultiplier = 1, rateMultiplier = 1;
        var features = new TreeSet<ResourceLocation>();
        for (var entry : entries) {
            var module = entry.stack(); var modifier = energy(module, armor);
            capacity += modifier.capacity(); rate += modifier.transferRate(); tier = Math.min(14, tier + modifier.tier()); provider |= modifier.provider();
            if (module.getItem() instanceof ArmorModule item) {
                storageMultiplier += item.extraStorageMultiplier(module, armor);
                rateMultiplier += item.extraTransferMultiplier(module, armor);
                features.addAll(item.features(module, armor));
            }
        }
        if (!Double.isFinite(storageMultiplier) || !Double.isFinite(rateMultiplier) || storageMultiplier < 0 || rateMultiplier < 0) throw new IllegalArgumentException("Invalid module multiplier");
        int packet = tier <= 1 ? 32 : tier >= 14 ? Integer.MAX_VALUE : (int) (32L << (2 * (tier - 1)));
        return new ArmorConfiguration(entries.stream().map(InstalledArmorModule::category).toList(),
            (int) Math.min(Integer.MAX_VALUE, capacity * storageMultiplier), (int) Math.min(Integer.MAX_VALUE, rate * rateMultiplier), packet, provider, enhanced, List.copyOf(features));
    }
    static void write(ItemStack armor, List<InstalledArmorModule> entries, ArmorConfiguration configuration) {
        armor.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(entries.stream().map(InstalledArmorModule::stack).toList()));
        armor.set(ArmorConfiguration.TYPE.get(), configuration);
    }
}
