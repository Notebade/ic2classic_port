package example;

import ic2.api.Ic2Api;
import ic2.api.item.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Visible, persistent test scene. No laser fixture is enabled by this entry point. */
@EventBusSubscriber(modid = ExampleAddon.MOD_ID, value = Dist.CLIENT)
public final class EquipmentVisualCheck {
    private static int ticks;
    private static boolean prepared;
    private static volatile boolean ready;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("ic2.visual.verify") || !System.getProperty("ic2.equipment.case", "sonar").equals("sonar")) return;
        var client = Minecraft.getInstance();
        var server = client.getSingleplayerServer();
        if (client.player == null || server == null || client.isPaused()) return;
        if (!prepared) {
            prepared = true;
            var id = client.player.getUUID();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(id);
                var level = player.serverLevel();
                for (var pos : BlockPos.betweenClosed(960, 80, 960, 1040, 80, 1040))
                    level.setBlock(pos, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 2);
                player.teleportTo(1000.5, 81, 1000.5);
                player.setYRot(180); player.setXRot(25); player.setNoGravity(false);
                player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                var shell = stack("nano_suit_helmet");
                var armor = (ModularArmor) shell.getItem();
                var helmet = armor.defaultArmor();
                var shield = stack("energy_shield_overlay_module");
                CustomData.update(DataComponents.CUSTOM_DATA, shield, tag -> tag.putBoolean("always", true));
                for (var module : java.util.List.of(stack("sonar_module"), shield, stack("energy_hud_module")))
                    if (!armor.install(helmet, ArmorModule.HUD, module)) throw new AssertionError("Visual fixture module rejected");
                armor.setCharge(helmet, 100000);
                for (var slot : EquipmentSlot.values()) if (slot.getType() == EquipmentSlot.Type.HUMANOID_ARMOR)
                    player.setItemSlot(slot, ItemStack.EMPTY);
                player.setItemSlot(EquipmentSlot.HEAD, helmet);
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                for (int distance : new int[] {5, 15, 30}) {
                    var cow = net.minecraft.world.entity.EntityType.COW.create(level);
                    cow.setPos(1000.5, 81, 1000.5 - distance); cow.setNoAi(true);
                    level.addFreshEntity(cow);
                }
                player.displayClientMessage(net.minecraft.network.chat.Component.literal("Сонар: Alt+H. Щит: F5. Волна повторяется каждые 12 секунд."), false);
                com.mojang.logging.LogUtils.getLogger().info("IC2 equipment visual fixture ready: sonar and energy shield");
                ready = true;
            });
        }
        if (!ready) return;
        int age = ++ticks;
        if (age == 20) {
            client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_BACK);
            client.player.setYRot(180); client.player.setXRot(25);
        }
        if (age % 240 == 60) {
            var id = client.player.getUUID();
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(id);
                var helmet = player.getItemBySlot(EquipmentSlot.HEAD);
                if (!(helmet.getItem() instanceof ModularArmor armor)) return;
                armor.setCharge(helmet, 100000);
                EquipmentControls.update(player, new EquipmentInput(false, false, false, true, false, true));
                ((ArmorTickItem) helmet.getItem()).tickArmor(helmet, player, EquipmentSlot.HEAD);
                EquipmentControls.clear(player);
            });
        }
        if (age == 85 || age == 140) net.minecraft.client.Screenshot.grab(client.gameDirectory,
            "sonar-shield-" + age + ".png", client.getMainRenderTarget(), message -> {});
    }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow()); }
}
