package example;

import ic2.api.Ic2Api;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ExampleAddon.MOD_ID, value = Dist.CLIENT)
public final class ClientAudioChecks {
    private static final BlockPos POS = new BlockPos(100, 82, 100);
    private static int age;
    private static SoundInstance muffled, normal, battery;
    private static volatile boolean recipeDiscoveryPassed;
    private ClientAudioChecks() {}
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) throws Exception {
        if (!Boolean.getBoolean("ic2.audio.verify")) return;
        var client = Minecraft.getInstance(); var server = client.getSingleplayerServer();
        if (client.player == null || client.level == null || server == null) return;
        age++;
        if (age == 1) server.execute(() -> {
            var level = server.overworld(); var player = server.getPlayerList().getPlayer(client.player.getUUID());
            player.setGameMode(GameType.CREATIVE);
            for (int x = 98; x <= 103; x++) for (int z = 94; z <= 102; z++) level.setBlockAndUpdate(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState());
            player.connection.teleport(100, 80, 96, 0, 0);
            level.setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(POS, Ic2Api.block(ResourceLocation.parse("ic2:induction_furnace")).orElseThrow().defaultBlockState());
            var entity = level.getBlockEntity(POS);
            var menu = ((MenuProvider) entity).createMenu(0, player.getInventory(), player);
            menu.slots.get(1).safeInsert(new ItemStack(Items.RAW_IRON, 64));
            menu.slots.get(5).safeInsert(new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:upgrade_muffler")).orElseThrow(), 2));
            var tag = entity.saveWithoutMetadata(level.registryAccess()); tag.putInt("energy", 10000); tag.putInt("heat", 10000);
            entity.loadWithComponents(tag, level.registryAccess());
        });
        if (age == 60) {
            muffled = sound(client);
            require(client.getSoundManager().isActive(muffled) && Math.abs(muffled.getVolume() - .32F) < .001F, "two mufflers produce a real .32-volume loop");
            server.execute(() -> replaceUpgrade(client, "upgrade_mute"));
        }
        if (age == 100) {
            require(((TickableSoundInstance) muffled).isStopped() && !client.getSoundManager().isActive(muffled), "mute stops the actual playing loop after network synchronization");
            server.execute(() -> replaceUpgrade(client, null));
        }
        if (age == 140) {
            normal = sound(client);
            require(normal != muffled && client.getSoundManager().isActive(normal) && Math.abs(normal.getVolume() - .5F) < .001F, "removing mute restarts the native loop at normal volume");
            server.execute(() -> server.overworld().setBlockAndUpdate(POS, Blocks.AIR.defaultBlockState()));
        }
        if (age == 180) {
            require(((TickableSoundInstance) normal).isStopped() && !client.getSoundManager().isActive(normal), "destroyed machine releases its loop");
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(client.player.getUUID());
                RecipeUnlockChecks.verifyPlayer(player); recipeDiscoveryPassed = true;
                player.getInventory().clearContent(); player.getInventory().selected = 0;
                var source = new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:re_battery")).orElseThrow());
                ((ic2.core.item.RechargeableItem) source.getItem()).setCharge(source, 10000);
                player.getInventory().setItem(0, source);
                player.getInventory().setItem(1, new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:quantum_accumulator")).orElseThrow()));
            });
        }
        if (age == 190) { require(recipeDiscoveryPassed, "real player inventory trigger unlocks the native fisher recipe"); client.player.getInventory().selected = 0; client.options.keyUse.setDown(true); client.gameMode.useItem(client.player, net.minecraft.world.InteractionHand.MAIN_HAND); }
        if (age == 210) {
            var field = Class.forName("ic2.core.machine.BatterySoundLoop").getDeclaredField("playing"); field.setAccessible(true);
            battery = (SoundInstance) field.get(null);
            require(battery != null && client.getSoundManager().isActive(battery), "portable hold-use plays its original battery loop; loop=" + battery + "; using=" + client.player.isUsingItem() + "; hand=" + client.player.getMainHandItem() + "; use=" + client.player.getUseItem());
            client.options.keyUse.setDown(false);
            client.gameMode.releaseUsingItem(client.player);
        }
        if (age == 230) {
            require(((TickableSoundInstance) battery).isStopped() && !client.getSoundManager().isActive(battery), "releasing the portable battery stops its real sound loop");
            com.mojang.logging.LogUtils.getLogger().info("IC2 client audio checks passed: recipe discovery, crafting statistics, machine/battery loops, modifiers and cleanup");
            client.stop();
        }
    }
    private static void replaceUpgrade(Minecraft client, String path) {
        var server = client.getSingleplayerServer(); var player = server.getPlayerList().getPlayer(client.player.getUUID());
        var menu = ((MenuProvider) server.overworld().getBlockEntity(POS)).createMenu(0, player.getInventory(), player);
        menu.slots.get(5).setByPlayer(ItemStack.EMPTY);
        if (path != null) menu.slots.get(5).safeInsert(new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + path)).orElseThrow()));
    }
    private static SoundInstance sound(Minecraft client) throws Exception {
        var entity = client.level.getBlockEntity(POS);
        var field = Class.forName("ic2.core.machine.MachineSoundLoop").getDeclaredField("PLAYING"); field.setAccessible(true);
        var sounds = (java.util.Map<?, ?>) ((java.util.Map<?, ?>) field.get(null)).get(entity);
        if (sounds == null || !(sounds.get("machine_furnace_operating") instanceof SoundInstance sound)) throw new AssertionError("native machine loop missing");
        return sound;
    }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
