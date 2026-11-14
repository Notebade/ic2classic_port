package example;

import ic2.api.Ic2Api;
import ic2.api.item.ElectricItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;

/** Packet-driven use/attack, rendered model variants and the real client sound engine. */
@EventBusSubscriber(modid = ExampleAddon.MOD_ID, value = Dist.CLIENT)
public final class NanoSaberClientCheck {
    private static int ticks, targetId, humStarts, powerups;
    private static SoundInstance hum;
    private static volatile Throwable failure;
    private static boolean enabled() {
        return Boolean.getBoolean("ic2.visual.verify") && System.getProperty("ic2.equipment.case", "").equals("saber");
    }
    @SubscribeEvent public static void sound(PlaySoundEvent event) {
        if (!enabled()) return;
        if (event.getName().equals("tools.nanosaber.nanosaber_idle")) { hum = event.getSound(); humStarts++; }
        if (event.getName().equals("tools.nanosaber.nanosaber_powerup")) powerups++;
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!enabled()) return;
        var client = Minecraft.getInstance(); var server = client.getSingleplayerServer();
        if (client.player == null || server == null || client.isPaused()) return;
        if (failure != null) throw new AssertionError("Nanosaber client fixture failed", failure);
        int age = ++ticks; var id = client.player.getUUID();
        if (age == 20) server.execute(() -> {
            try {
                NanoSaberChecks.run(server.overworld());
                var player = server.getPlayerList().getPlayer(id); var level = player.serverLevel();
                for (int x = -6; x <= 6; x++) for (int z = -6; z <= 6; z++) {
                    var pos = new net.minecraft.core.BlockPos(1300 + x, 149, 1000 + z);
                    level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.SMOOTH_STONE.defaultBlockState());
                    for (int y = 1; y <= 5; y++) level.setBlockAndUpdate(pos.above(y), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
                }
                player.getInventory().clearContent(); player.closeContainer(); player.setInvulnerable(true);
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
                player.teleportTo(level, 1300.5, 150, 1000.5, 0, 0);
                var saber = new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:nano_saber")).orElseThrow());
                ((ElectricItem) saber.getItem()).setCharge(saber, 40000); player.setItemInHand(InteractionHand.MAIN_HAND, saber);
                var target = net.minecraft.world.entity.EntityType.IRON_GOLEM.create(level);
                target.moveTo(1300.5, 150, 1003); target.setNoAi(true); target.setPersistenceRequired();
                level.addFreshEntity(target); targetId = target.getId();
                level.setDayTime(6000);
            } catch (Throwable error) { failure = error; }
        });
        if (age == 45) { client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON); client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND); }
        if (age == 80) {
            require(active(client), "right click activates and synchronizes saber");
            require(humStarts == 1 && powerups == 1 && hum != null && hum.isLooping() && client.getSoundManager().isActive(hum),
                "one activation sound and one continuously playing hum");
            var sprites = new java.util.HashSet<String>();
            for (int i = 0; i < 100; i++) sprites.add(client.getItemRenderer().getModel(client.player.getMainHandItem(), client.level, client.player, i)
                .getParticleIcon().contents().name().toString());
            require(sprites.equals(java.util.Set.of("ic2:item/tools/nano_saber/active_0", "ic2:item/tools/nano_saber/active_1")), "both source blade models flicker without a missing sprite");
            capture(client, "nanosaber-active.png");
        }
        if (age == 95) client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        if (age == 115) {
            require(active(client) && humStarts == 1, "plain right click leaves saber and original hum active");
            var target = client.level.getEntity(targetId); require(target != null, "combat target tracked by client");
            client.gameMode.attack(client.player, target); client.player.swing(InteractionHand.MAIN_HAND);
        }
        if (age == 130) server.execute(() -> {
            try {
                var target = (net.minecraft.world.entity.LivingEntity) server.overworld().getEntity(targetId);
                require(target != null && target.getHealth() == 80, "real client attack deals 20 damage with active saber");
                com.mojang.logging.LogUtils.getLogger().info("IC2 nanosaber client attack passed: 20 damage");
            } catch (Throwable error) { failure = error; }
        });
        if (age == 145) alt(client, true);
        if (age == 160) client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        if (age == 180) {
            alt(client, false);
            require(!active(client) && !client.getSoundManager().isActive(hum), "Alt+right click deactivates saber and stops hum");
            var sprite = client.getItemRenderer().getModel(client.player.getMainHandItem(), client.level, client.player, 0).getParticleIcon().contents().name();
            require(sprite.equals(ResourceLocation.parse("ic2:item/tools/nano_saber/off")), "inactive model restored");
            capture(client, "nanosaber-off.png");
        }
        if (age == 200) client.gameMode.useItem(client.player, InteractionHand.MAIN_HAND);
        if (age == 225) {
            require(active(client) && humStarts == 2 && client.getSoundManager().isActive(hum), "second activation starts exactly one fresh hum");
            client.player.getInventory().selected = 1;
        }
        if (age == 250) {
            require(!client.getSoundManager().isActive(hum), "unequipping saber stops the moving hum");
            client.player.getInventory().selected = 0;
            com.mojang.logging.LogUtils.getLogger().info("IC2 equipment visual fixture ready: nanosaber controls, combat, armor discharge, both blade sprites and loop sound passed");
        }
    }
    private static void alt(Minecraft client, boolean down) {
        java.util.Arrays.stream(client.options.keyMappings).filter(key -> key.getName().equals("key.ic2.alt")).findFirst().orElseThrow().setDown(down);
    }
    private static boolean active(Minecraft client) { return client.player.getMainHandItem().canPerformAction(net.neoforged.neoforge.common.ItemAbilities.SWORD_SWEEP); }
    private static void capture(Minecraft client, String file) { net.minecraft.client.Screenshot.grab(client.gameDirectory, file, client.getMainRenderTarget(), message -> {}); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
