package example;

import ic2.api.Ic2Api;
import ic2.api.item.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

final class FallArmorChecks {
    static void run(ServerLevel level) throws ReflectiveOperationException {
        var fallRule = level.getGameRules().getRule(net.minecraft.world.level.GameRules.RULE_FALL_DAMAGE);
        boolean fallDamage = fallRule.get();
        fallRule.set(true, level.getServer());
        var player = new net.neoforged.neoforge.common.util.FakePlayer(level,
            new com.mojang.authlib.GameProfile(java.util.UUID.fromString("1725625c-1634-4bd9-8b1d-e7ab71cbda4e"), "FallFixture")) {
            @Override public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource source) { return false; }
        };
        // FakePlayer.tick() is empty, so its initial spawn protection never expires.
        // Dedicated servers bypass that protection for falls; integrated servers do not.
        var spawnProtection = net.minecraft.server.level.ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
        spawnProtection.setAccessible(true);
        spawnProtection.setInt(player, 0);
        var slots = java.util.List.of(EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD);
        var saved = slots.stream().map(s -> player.getItemBySlot(s).copy()).toList(); var gameMode = player.gameMode.getGameModeForPlayer(); float health = player.getHealth(); int invulnerable = player.invulnerableTime;
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); for (var slot : slots) player.setItemSlot(slot, ItemStack.EMPTY);
            var boots = boots(); var armor = (ModularArmor) boots.getItem(); armor.setEnhanced(boots, true); armor.setCharge(boots, 4000); player.setItemSlot(EquipmentSlot.FEET, boots);
            require(((FallProtectiveArmor) armor).absorbFall(boots, player, 5, true) == 5 && armor.charge(boots) == 4000, "fall protection preview does not spend EU");
            fall(player, 8);
            require(player.getHealth() == 20 && armor.charge(boots) == 0, "enhanced Nano boots fully absorb a real five-damage fall for exactly 4000 EU");
            armor.setCharge(boots, 1600); fall(player, 8);
            require(player.getHealth() == 17 && armor.charge(boots) == 0, "partial armor charge buys exactly two fall-damage points without free remaining protection: health=" + player.getHealth() + ", EU=" + armor.charge(boots));
            armor.setEnhanced(boots, false); armor.setCharge(boots, 8800); fall(player, 23);
            require(player.getHealth() == 11 && armor.charge(boots) == 0, "regular long-fall protection reduces eleven damage and actually spends 8800 EU");
            var bare = stack("nano_suit_boots"); player.setItemSlot(EquipmentSlot.FEET, bare); fall(player, 8);
            require(player.getHealth() == 15, "unpowered Nano boots do not grant free fall reduction");
            var rubber = stack("armor_hazmat_boots"); player.setItemSlot(EquipmentSlot.FEET, rubber); fall(player, 8);
            require(player.getHealth() == 20 && rubber.getDamageValue() == 3, "source rubber boots absorb five fall damage for three durability");
            var addon = new ItemStack(ExampleAddon.FALL_TEST_BOOTS.get()); player.setItemSlot(EquipmentSlot.FEET, addon); fall(player, 8);
            require(player.getHealth() == 17 && addon.getOrDefault(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.EMPTY).copyTag().getInt("falls") == 1, "separately compiled addon fall protection runs exactly once before actual native damage");
        } finally {
            fallRule.set(fallDamage, level.getServer());
            for (int i = 0; i < slots.size(); i++) player.setItemSlot(slots.get(i), saved.get(i)); player.setGameMode(gameMode); player.setHealth(health); player.invulnerableTime = invulnerable;
        }
    }
    private static void fall(net.minecraft.server.level.ServerPlayer player, float distance) { player.setHealth(20); player.invulnerableTime = 0; player.causeFallDamage(distance, 1, player.damageSources().fall()); }
    private static ItemStack boots() { var shell = stack("nano_suit_boots"); return ((ModularArmor) shell.getItem()).defaultArmor(); }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow()); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
