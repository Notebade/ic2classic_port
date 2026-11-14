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
    private enum Kind { AIR, CHARGER, PROTECTION, EXTINGUISH, GOLD, FEED, LAVA, ENERGY_HUD, SHIELD }
    private final Kind kind;
    private PassiveArmorModule(Properties properties, Kind kind) { super(properties); this.kind = kind; }
    static void register(DeferredRegister.Items items) {
        items.registerSimpleItem("base_module");
        items.registerItem("air_refill_module", p -> new PassiveArmorModule(p, Kind.AIR));
        items.registerItem("charger_module", p -> new PassiveArmorModule(p, Kind.CHARGER));
        items.registerItem("protective_module", p -> new PassiveArmorModule(p, Kind.PROTECTION));
        items.registerItem("extinguish_module", p -> new PassiveArmorModule(p, Kind.EXTINGUISH));
        items.registerItem("golden_module", p -> new PassiveArmorModule(p, Kind.GOLD));
        items.registerItem("auto_feeder_module", p -> new PassiveArmorModule(p, Kind.FEED));
        items.registerItem("lava_module", p -> new PassiveArmorModule(p, Kind.LAVA));
        items.registerItem("energy_hud_module", p -> new PassiveArmorModule(p, Kind.ENERGY_HUD));
        items.registerItem("energy_shield_overlay_module", p -> new PassiveArmorModule(p, Kind.SHIELD));
    }
    @Override public ResourceLocation category(ItemStack module) {
        return switch (kind) { case CHARGER -> CHARGER; case PROTECTION, EXTINGUISH, LAVA -> ANY; case ENERGY_HUD, SHIELD -> HUD; default -> GENERIC; };
    }
    @Override public boolean supportsArmor(ItemStack module, ItemStack armor, EquipmentSlot slot) {
        return (kind != Kind.AIR && kind != Kind.FEED && kind != Kind.ENERGY_HUD && kind != Kind.SHIELD) || slot == EquipmentSlot.HEAD;
    }
    @Override public ArmorEnergyModifier energyModifier(ItemStack module, ItemStack armor) { return new ArmorEnergyModifier(0, 0, 0, kind == Kind.CHARGER); }
    @Override public Set<ResourceLocation> features(ItemStack module, ItemStack armor) {
        return switch (kind) {
            case GOLD -> Set.of(ArmorModule.id("golden"));
            case LAVA -> Set.of(ArmorModule.id("lava_protection"));
            case ENERGY_HUD -> Set.of(ArmorModule.id("energy_hud"));
            case SHIELD -> module.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
                net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getBoolean("always")
                ? Set.of(ArmorModule.id("energy_shield_overlay"), ArmorModule.id("energy_shield_always"))
                : Set.of(ArmorModule.id("energy_shield_overlay"));
            default -> Set.of();
        };
    }
    @Override public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level,
            Player player, net.minecraft.world.InteractionHand hand) {
        if (kind != Kind.SHIELD || !player.isShiftKeyDown()) return super.use(level, player, hand);
        var stack = player.getItemInHand(hand);
        toggleShield(stack, player);
        return net.minecraft.world.InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
    @Override public boolean useOnArmor(ItemStack module, ItemStack armor, Player player) {
        if (kind != Kind.SHIELD || !player.isShiftKeyDown()) return false;
        toggleShield(module, player);
        return true;
    }
    private static void toggleShield(ItemStack module, Player player) {
        if (player.level().isClientSide) return;
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            module, tag -> tag.putBoolean("always", !tag.getBoolean("always")));
        boolean always = module.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA).copyTag().getBoolean("always");
        player.displayClientMessage(net.minecraft.network.chat.Component.translatable(always
            ? "tooltip.item.ic2.module_energy_shield.enable" : "tooltip.item.ic2.module_energy_shield.disable"), false);
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context,
            java.util.List<net.minecraft.network.chat.Component> tooltip, net.minecraft.world.item.TooltipFlag flag) {
        if (kind != Kind.SHIELD) return;
        tooltip.add(net.minecraft.network.chat.Component.literal("Shift + ").append(net.minecraft.network.chat.Component.translatable("key.mouse.right"))
            .append(": ").append(net.minecraft.network.chat.Component.translatable("tooltip.item.ic2.module_energy_shield")));
        boolean always = stack.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA,
            net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getBoolean("always");
        tooltip.add(net.minecraft.network.chat.Component.translatable(always
            ? "tooltip.item.ic2.module_energy_shield.enable" : "tooltip.item.ic2.module_energy_shield.disable"));
    }
    @Override public void tickModule(ItemStack module, ItemStack armor, Player player) {
        switch (kind) {
            case AIR -> { if (player.getAirSupply() < 100 && spend(armor, 2000)) player.setAirSupply(Math.min(player.getMaxAirSupply(), player.getAirSupply() + 200)); }
            case EXTINGUISH -> player.clearFire();
            case FEED -> feed(armor, player);
            case PROTECTION -> {
                removeEffect(armor, player, MobEffects.POISON, 10000);
                var radiation = player.level().registryAccess().registryOrThrow(Registries.MOB_EFFECT).getHolder(ResourceLocation.parse("ic2:radiation"));
                radiation.ifPresent(effect -> removeEffect(armor, player, effect, 20000));
                removeEffect(armor, player, MobEffects.WITHER, 25000);
            }
            default -> { }
        }
    }
    private static void feed(ItemStack armor, Player player) {
        if (!player.getFoodData().needsFood() || !ElectricEquipment.canUse(armor, 1000)) return;
        if (FoodStorageModule.feed(player, armor, player.getItemBySlot(EquipmentSlot.CHEST))) return;
        var inventory = player.getInventory();
        for (var box : inventory.items) {
            if (box.getItem() instanceof Toolbox toolbox && feedFromToolbox(armor, player, box, toolbox)) return;
        }
        for (int slot = 0; slot < inventory.items.size(); slot++) {
            var stack = inventory.getItem(slot);
            var food = stack.getFoodProperties(player);
            if (food == null || !ElectricEquipment.use(armor, 1000, player)) continue;
            var serving = inventory.removeItem(slot, 1);
            var remainder = serving.finishUsingItem(player.level(), player);
            if (!remainder.isEmpty()) inventory.placeItemBackInInventory(remainder);
            return;
        }
    }
    private static boolean feedFromToolbox(ItemStack armor, Player player, ItemStack box, Toolbox toolbox) {
        for (int slot = 0; slot < toolbox.slots(); slot++) {
            var stack = toolbox.stored(box, slot);
            if (stack.isEmpty()) continue;
            var food = stack.getFoodProperties(player);
            if (food == null) continue;
            var remaining = stack.copyWithCount(stack.getCount() - 1);
            if (!toolbox.store(box, slot, remaining)) continue;
            if (!ElectricEquipment.use(armor, 1000, player)) {
                toolbox.store(box, slot, stack);
                return false;
            }
            var remainder = stack.copyWithCount(1).finishUsingItem(player.level(), player);
            if (!remainder.isEmpty() && (!remaining.isEmpty() || !toolbox.store(box, slot, remainder))) {
                player.getInventory().placeItemBackInInventory(remainder);
            }
            player.getInventory().setChanged();
            return true;
        }
        return false;
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
