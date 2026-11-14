package example;

import ic2.api.Ic2Api;
import ic2.api.item.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Six visible sprays through the player's normal server interaction path. Leaves the scene open. */
@EventBusSubscriber(modid = ExampleAddon.MOD_ID, value = Dist.CLIENT)
public final class FoamClientCheck {
    private static int ticks;
    private static volatile Throwable failure;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("ic2.visual.verify") || !System.getProperty("ic2.equipment.case", "").equals("foam")) return;
        var client = Minecraft.getInstance(); var server = client.getSingleplayerServer();
        if (client.player == null || server == null || client.isPaused()) return;
        if (failure != null) throw new AssertionError("Foam client fixture failed", failure);
        int age = ++ticks; var playerId = client.player.getUUID();
        if (age == 20) {
            client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
            server.execute(() -> {
                try {
                    var player = server.getPlayerList().getPlayer(playerId); var level = player.serverLevel();
                    player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); player.setNoGravity(true); player.setInvulnerable(true);
                    for (var slot : net.minecraft.world.entity.EquipmentSlot.values())
                        if (slot.getType() == net.minecraft.world.entity.EquipmentSlot.Type.HUMANOID_ARMOR) player.setItemSlot(slot, ItemStack.EMPTY);
                    for (int index = 0; index < 6; index++) {
                        var origin = origin(index);
                        for (var pos : BlockPos.betweenClosed(origin.offset(-5, -1, -7), origin.offset(5, 5, 6)))
                            level.setBlock(pos, pos.getY() == 169 ? Blocks.STONE.defaultBlockState() : Blocks.AIR.defaultBlockState(), 2 | 16);
                    }
                    player.connection.teleport(1100.5, 171, 995.5, 0, 30);
                    FoamChecks.run(level);
                    com.mojang.logging.LogUtils.getLogger().info("IC2 foam integrated-client behavior checks passed");
                } catch (Throwable error) { failure = error; }
            });
        }
        int stage = (age - 60) / 100;
        if (age >= 60 && stage < 6 && (age - 60) % 100 == 0) server.execute(() -> {
            try {
                var player = server.getPlayerList().getPlayer(playerId); var origin = origin(stage);
                var sprayer = stack("electric_sprayer");
                ((ElectricItem) sprayer.getItem()).setCharge(sprayer, 10000);
                ((FoamContainer) sprayer.getItem()).setFoam(sprayer, 1000);
                ((ConfigurableTool) sprayer.getItem()).setMode(sprayer, Integer.toString(1 << stage));
                player.setItemInHand(InteractionHand.MAIN_HAND, sprayer);
                player.connection.teleport(origin.getX() + .5, 171, 995.5, 0, 30);
                player.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(Component.literal("Пена: режим " + (1 << stage))));
            } catch (Throwable error) { failure = error; }
        });
        if (age >= 60 && stage < 6 && (age - 60) % 100 == 35) server.execute(() -> {
            try {
                var player = server.getPlayerList().getPlayer(playerId); var level = player.serverLevel();
                var origin = origin(stage); var sprayer = player.getMainHandItem();
                var hit = new BlockHitResult(Vec3.atCenterOf(origin.below()), Direction.UP, origin.below(), false);
                var result = player.gameMode.useItemOn(player, level, sprayer, InteractionHand.MAIN_HAND, hit);
                int count = 0;
                var wet = Ic2Api.block(ResourceLocation.parse("ic2:wet_construction_foam")).orElseThrow();
                for (var pos : BlockPos.betweenClosed(origin.offset(-6, -6, -6), origin.offset(6, 6, 6))) if (level.getBlockState(pos).is(wet)) count++;
                int expected = 1 << stage;
                if (!result.consumesAction() || count != expected || ((ElectricItem) sprayer.getItem()).charge(sprayer) != 10000 - expected * 5)
                    throw new AssertionError("Visible spray " + expected + ": result=" + result + ", placed=" + count);
                player.swing(InteractionHand.MAIN_HAND, true);
                player.displayClientMessage(Component.literal("Поставлено " + count + ", потрачено " + (expected * 5) + " EU"), false);
                com.mojang.logging.LogUtils.getLogger().info("IC2 foam client mode passed: {}", expected);
            } catch (Throwable error) { failure = error; }
        });
        if (age >= 60 && stage < 6 && (age - 60) % 100 == 55)
            net.minecraft.client.Screenshot.grab(client.gameDirectory, "foam-mode-" + (1 << stage) + ".png", client.getMainRenderTarget(), message -> {});
        if (age == 680) com.mojang.logging.LogUtils.getLogger().info("IC2 equipment visual fixture ready: all six foam modes passed");
    }
    private static BlockPos origin(int index) { return new BlockPos(1100 + index * 16, 170, 1000); }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow()); }
}
