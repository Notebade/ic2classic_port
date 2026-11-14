package example;

import ic2.api.Ic2Api;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

@net.neoforged.fml.common.EventBusSubscriber(modid = ExampleAddon.MOD_ID)
public final class FoodCanEffectsChecks {
    @net.neoforged.bus.api.SubscribeEvent public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if (!Boolean.getBoolean("ic2.cans.verify")) return;
        try { run(event.getServer().overworld()); com.mojang.logging.LogUtils.getLogger().info("IC2 focused food can effects checks passed"); }
        finally { event.getServer().halt(false); }
    }
    static void run(net.minecraft.server.level.ServerLevel level) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var effects = player.getActiveEffects().stream().map(net.minecraft.world.effect.MobEffectInstance::new).toList();
        int food = player.getFoodData().getFoodLevel(); float saturation = player.getFoodData().getSaturationLevel(), health = player.getHealth();
        var mode = player.gameMode.getGameModeForPlayer();
        try {
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            require(stack("").getUseDuration(player) == 20, "source food can takes twenty ticks to eat");
            int affected = 0;
            for (int attempt = 0; attempt < 64; attempt++) {
                player.removeAllEffects(); level.random.setSeed(attempt); player.getRandom().setSeed(attempt);
                stack("_bad").finishUsingItem(level, player);
                require(player.hasEffect(MobEffects.CONFUSION) == player.hasEffect(MobEffects.HUNGER), "bad food rolls once for both effects, never separately");
                if (player.hasEffect(MobEffects.HUNGER)) affected++;
            }
            require(affected > 0 && affected < 64, "bad food can both apply and avoid its effects");
            player.removeAllEffects(); player.setHealth(16); player.getFoodData().setFoodLevel(16);
            var remainder = stack("_enchanted_golden_apple").finishUsingItem(level, player);
            require(player.getHealth() == 18 && player.getFoodData().getFoodLevel() == 18 && remainder.is(Ic2Api.item(ResourceLocation.parse("ic2:tin_can")).orElseThrow()),
                "can heals two, feeds two and returns empty can");
            require(player.getEffect(MobEffects.REGENERATION).getDuration() == 100 && player.getEffect(MobEffects.REGENERATION).getAmplifier() == 1
                && player.getEffect(MobEffects.ABSORPTION).getAmplifier() == 3 && player.getEffect(MobEffects.FIRE_RESISTANCE).getDuration() == 1500,
                "enchanted apple can retains original effects");
            player.removeAllEffects(); stack("_pufferfish").finishUsingItem(level, player);
            require(player.getEffect(MobEffects.POISON).getDuration() == 1200 && player.getEffect(MobEffects.POISON).getAmplifier() == 3
                && player.getEffect(MobEffects.HUNGER).getDuration() == 300 && player.getEffect(MobEffects.CONFUSION).getAmplifier() == 1,
                "pufferfish effects match source durations and amplifiers");
        } finally {
            player.removeAllEffects(); effects.forEach(player::addEffect); player.setHealth(health);
            player.getFoodData().setFoodLevel(food); player.getFoodData().setSaturation(saturation); player.setGameMode(mode);
        }
    }
    private static ItemStack stack(String suffix) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:filled_tin_can" + suffix)).orElseThrow()); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
