package example;

import ic2.api.Ic2Api;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/**
 * Opt-in screenshots of actual client screens in an isolated integrated-server world.
 */
@EventBusSubscriber(modid = ExampleAddon.MOD_ID, value = Dist.CLIENT)
public final class ClientRenderChecks {
    private static final String[] DEVICES = {"monitor", "reactor_planner", "nuclear_reactor", "steam_reactor", "solar_turbine", "slag_generator", "windmill", "liquid_fuel_generator", "plasmafier", "fuel_boiler", "fusion_reactor", "steam_tunnel", "machine_buffer", "personal_chest", "toolbox", "chargepad_lv", "chargepad_mv", "chargepad_hv", "chargepad_fission", "chunkloader", "uranium_enricher", "uranium_enricher_jei"};
    private static int age;
    private static boolean finished;
    private static final BlockPos ORIGIN = new BlockPos(100, 82, 100);

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("ic2.client.verify") || finished) return;
        var client = Minecraft.getInstance();
        var server = client.getSingleplayerServer();
        if (client.player == null || client.level == null || server == null) return;
        age++;
        if (age == 1) server.execute(()->prepare(client));
        if (age < 80) return;
        int step = (age - 80) / 40;
        int offset = (age - 80) % 40;
        if (step > DEVICES.length) {
            finished = true;
            com.mojang.logging.LogUtils.getLogger().info("IC2 client render checks passed: {} screens", DEVICES.length);
            client.stop();
            return;
        }
        if (step == DEVICES.length) {
            if (offset == 0) server.execute(()->{
                var player = server.getPlayerList().getPlayer(client.player.getUUID());
                player.closeContainer();
                player.connection.teleport(100.5, 80, 96, 0, -10);
            });
            if (offset == 20) {
                var panel = client.level.getBlockEntity(ORIGIN.east()).getUpdateTag(client.level.registryAccess());
                if (panel.getInt("color") != 6 || panel.getInt("scale") != 5 || panel.getInt("screen_width") != 2)
                    throw new AssertionError("Monitor wall appearance did not synchronize to the real client");
                Screenshot.grab(client.gameDirectory, "ic2-monitor-wall.png", client.getMainRenderTarget(), (message)->{});
            }
            return;
        }
        if (offset == 0) {
            server.execute(()->{
                var player = server.getPlayerList().getPlayer(client.player.getUUID());
                player.connection.teleport(100 + step * 2, 80, 96, 0, 0);
                if (DEVICES[step].equals("toolbox")) {
                    var box = new net.minecraft.world.item.ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:toolbox")).orElseThrow());
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, box);
                    box.getItem().use(server.overworld(), player, net.minecraft.world.InteractionHand.MAIN_HAND);
                    return;
                }
                if (DEVICES[step].equals("uranium_enricher_jei")) {
                    player.closeContainer(); client.execute(RecipeJeiChecks::showEnrichment); return;
                }
                var entity = server.overworld().getBlockEntity(ORIGIN.offset(step * 2, 0, 0));
                var opened = player.openMenu((MenuProvider)entity);
                if (opened.isEmpty()) throw new AssertionError("Server refused menu " + DEVICES[step]);
            });
        }
        if (offset == 20) {
            if (DEVICES[step].equals("uranium_enricher_jei")) {
                if (client.screen == null || !client.screen.getClass().getName().contains("RecipesGui")) throw new AssertionError("Expected JEI recipes screen");
            } else if (!(client.screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)) throw new AssertionError("Expected actual machine screen for " + DEVICES[step] + ", got " + client.screen);
            Screenshot.grab(client.gameDirectory, "ic2-" + DEVICES[step] + ".png", client.getMainRenderTarget(), (message)->com.mojang.logging.LogUtils.getLogger().info("IC2 screenshot: {}", message.getString()));
        }
    }

    private static void prepare(Minecraft client) {
        var server = client.getSingleplayerServer();
        var level = server.overworld();
        var player = server.getPlayerList().getPlayer(client.player.getUUID());
        player.setGameMode(GameType.CREATIVE);
        for (int x = 96; x < 106 + DEVICES.length * 2; x++) for (int z = 94; z < 104; z++) {
            var floor = new BlockPos(x, 79, z);
            level.getChunkAt(floor);
            level.setBlockAndUpdate(floor, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
        }
        player.connection.teleport(100, 80, 96, 0, 0);
        for (int i = 0; i < DEVICES.length; i++) {
            if (DEVICES[i].equals("toolbox") || DEVICES[i].endsWith("_jei")) continue;
            var pos = ORIGIN.offset(i * 2, 0, 0);
            level.getChunkAt(pos);
            var state = Ic2Api.block(ResourceLocation.parse("ic2:" + DEVICES[i])).orElseThrow().defaultBlockState();
            if (state.hasProperty(BlockStateProperties.FACING)) state = state.setValue(BlockStateProperties.FACING, Direction.NORTH);
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, state);
            var entity = level.getBlockEntity(pos);
            var tag = entity.saveWithFullMetadata(level.registryAccess());
            if (DEVICES[i].equals("monitor")) {
                tag.putInt("energy", 1000);
                tag.putBoolean("redstone", false);
            }
            entity.loadWithComponents(tag, level.registryAccess());
        }
        for (int x = 0; x < 2; x++) for (int y = 0; y < 2; y++) {
            var pos = ORIGIN.offset(x, -y, 0);
            level.setBlockAndUpdate(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
            level.setBlockAndUpdate(pos, Ic2Api.block(ResourceLocation.parse("ic2:monitor")).orElseThrow().defaultBlockState());
            var entity = level.getBlockEntity(pos);
            var tag = entity.saveWithFullMetadata(level.registryAccess());
            tag.putInt("energy", 1000);
            tag.putBoolean("redstone", false);
            entity.loadWithComponents(tag, level.registryAccess());
        }
        var reactor = ORIGIN.south(3);
        level.setBlockAndUpdate(reactor, Ic2Api.block(ResourceLocation.parse("ic2:nuclear_reactor")).orElseThrow().defaultBlockState());
        var card = new net.minecraft.world.item.ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:reactor_card")).orElseThrow());
        net.minecraft.world.item.component.CustomData.update(net.minecraft.core.component.DataComponents.CUSTOM_DATA, card, (tag)->{
            tag.putString("reactor_dimension", level.dimension().location().toString());
            tag.putLong("reactor_position", reactor.asLong());
        });
        level.getCapability(net.neoforged.neoforge.capabilities.Capabilities.ItemHandler.BLOCK, ORIGIN, Direction.UP).insertItem(0, card, false);
        var menu = ((MenuProvider)level.getBlockEntity(ORIGIN)).createMenu(1, player.getInventory(), player);
        for (int button : new int[]{1, 3, 4, 5}) menu.clickMenuButton(player, button);
    }
}
