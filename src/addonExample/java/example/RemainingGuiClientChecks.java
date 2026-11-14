package example;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Isolated client/server opening regression for previously unregistered screens and storage menus. */
@EventBusSubscriber(modid = ExampleAddon.MOD_ID, value = Dist.CLIENT)
public final class RemainingGuiClientChecks {
    private static final String[] BLOCKS = {"miner", "overclocked_pump", "ranged_pump", "electric_enchanter", "machine_buffer", "personal_chest", "armor_configurator"};
    private static final String[] ITEMS = {"toolbox", "carbon_box", "iridium_box"};
    private static final BlockPos POS = new BlockPos(100, 80, 100);
    private static int age, index;
    private static boolean done;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("ic2.remainingGui.verify") || done) return;
        var client = Minecraft.getInstance();
        var server = client.getSingleplayerServer();
        if (server == null || client.player == null) return;
        int phase = ++age % 50;
        if (phase == 1) server.execute(() -> {
            var player = server.getPlayerList().getPlayer(client.player.getUUID());
            player.closeContainer();
            for (int x = 98; x <= 102; x++) for (int z = 96; z <= 102; z++)
                server.overworld().setBlockAndUpdate(new BlockPos(x, 79, z), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
            player.connection.teleport(100, 80, 98, 0, 0);
            if (index < BLOCKS.length) {
                var block = BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("ic2", BLOCKS[index]));
                server.overworld().setBlockAndUpdate(POS, block.defaultBlockState());
                player.openMenu((MenuProvider) server.overworld().getBlockEntity(POS));
            } else {
                var item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("ic2", ITEMS[index - BLOCKS.length]));
                player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new net.minecraft.world.item.ItemStack(item));
                item.use(server.overworld(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
            }
        });
        if (phase == 30) {
            if (!(client.screen instanceof AbstractContainerScreen<?> screen)) throw new AssertionError("No screen for " + index);
            String expected = index < 3 ? "ExtractionScreen" : index == 3 ? "ElectricEnchanterScreen" : index == 6 ? "ArmorConfiguratorScreen" : "StorageScreen";
            if (!screen.getClass().getSimpleName().equals(expected)) throw new AssertionError("Wrong screen: " + screen.getClass());
            if (screen.getMenu() != client.player.containerMenu) throw new AssertionError("Client/server menu mismatch");
            Screenshot.grab(client.gameDirectory, "remaining-gui-" + index + ".png", client.getMainRenderTarget(), ignored -> {});
        }
        if (phase == 49 && ++index == BLOCKS.length + ITEMS.length) {
            done = true;
            com.mojang.logging.LogUtils.getLogger().info("IC2 remaining GUI checks passed: 10 production menus opened in client");
            client.stop();
        }
    }
}
