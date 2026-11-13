package ic2.core.equipment;

import ic2.api.item.*;
import java.util.Set;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

final class PassiveArmorModule extends Item implements ArmorModule {
    private enum Kind { AIR, CHARGER, PROTECTION, EXTINGUISH, GOLD }
    private final Kind kind;
    private PassiveArmorModule(Properties properties, Kind kind) { super(properties); this.kind = kind; }
    static void register(DeferredRegister.Items items) {
        items.registerSimpleItem("base_module");
        items.registerItem("air_refill_module", p -> new PassiveArmorModule(p, Kind.AIR));
        items.registerItem("charger_module", p -> new PassiveArmorModule(p, Kind.CHARGER));
        items.registerItem("protective_module", p -> new PassiveArmorModule(p, Kind.PROTECTION));
        items.registerItem("extinguish_module", p -> new PassiveArmorModule(p, Kind.EXTINGUISH));
        items.registerItem("golden_module", p -> new PassiveArmorModule(p, Kind.GOLD));
    }
    @Override public ResourceLocation category(ItemStack module) {
        return switch (kind) { case CHARGER -> CHARGER; case PROTECTION, EXTINGUISH -> ANY; default -> GENERIC; };
    }
    @Override public boolean supportsArmor(ItemStack module, ItemStack armor, EquipmentSlot slot) {
        return kind != Kind.AIR || slot == EquipmentSlot.HEAD;
    }
    @Override public ArmorEnergyModifier energyModifier(ItemStack module, ItemStack armor) { return new ArmorEnergyModifier(0, 0, 0, kind == Kind.CHARGER); }
    @Override public Set<ResourceLocation> features(ItemStack module, ItemStack armor) { return kind == Kind.GOLD ? Set.of(ArmorModule.id("golden")) : Set.of(); }
    @Override public void tickModule(ItemStack module, ItemStack armor, Player player) {
        switch (kind) {
            case AIR -> { if (player.getAirSupply() < 100 && spend(armor, 2000)) player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + 200)); }
            case EXTINGUISH -> player.clearFire();
            case PROTECTION -> {
                removeEffect(armor, player, MobEffects.POISON, 10000);
                var radiation = player.level().registryAccess().registryOrThrow(Registries.MOB_EFFECT).getHolder(ResourceLocation.parse("ic2:radiation"));
                radiation.ifPresent(effect -> removeEffect(armor, player, effect, 20000));
                removeEffect(armor, player, MobEffects.WITHER, 25000);
            }
            default -> { }
        }
    }
    private static void removeEffect(ItemStack armor, Player player, Holder<MobEffect> effect, int baseCost) {
        var current = player.getEffect(effect);
        if (current != null && spend(armor, (int) Math.min(Integer.MAX_VALUE, (long) baseCost * (current.getAmplifier() + 1)))) player.removeEffect(effect);
    }
    private static boolean spend(ItemStack armor, int amount) {
        if (!(armor.getItem() instanceof ElectricItem item) || item.charge(armor) < amount) return false;
        item.setCharge(armor, item.charge(armor) - amount); return true;
    }
}
