package example;

import ic2.api.Ic2Api;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Opt-in real client/server checks; runs only inside an isolated copied world. */
@EventBusSubscriber(modid = ExampleAddon.MOD_ID, value = Dist.CLIENT)
public final class RemasteredClientChecks {
    private static final BlockPos POS = new BlockPos(100, 82, 100);
    private static int age;
    private static boolean finished;

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("ic2.remastered.verify") || finished) return;
        var client = Minecraft.getInstance();
        var server = client.getSingleplayerServer();
        if (server == null || client.player == null || client.level == null) return;
        age++;
        if (age == 1) server.execute(() -> prepare(client));
        if (age == 60) server.execute(() -> {
            var player = server.getPlayerList().getPlayer(client.player.getUUID());
            player.connection.teleport(100, 80, 98, 0, 0);
            player.openMenu((MenuProvider) server.overworld().getBlockEntity(POS));
        });
        if (age == 80 || age == 110 || age == 140 || age == 170) {
            int tab = (age - 80) / 30;
            var screen = screen(client);
            if (tab == 0) {
                if (!tab(client, 0).getClass().getSimpleName().equals("RemasteredButton"))
                    throw new AssertionError("Machine tabs still use vanilla button rendering");
                for (int i = 3; i < 39; i++) {
                    var slot = screen.getMenu().getSlot(i);
                    if (slot.x - 2 < 42 || slot.x + 18 > 214 || slot.y - 2 < 160 || slot.y + 18 > 236)
                        throw new AssertionError("Player slot escapes inventory frame: " + i);
                }
            }
            if (tab > 0) tab(client, tab).onPress();
            long active = screen.getMenu().slots.stream().filter(slot -> slot.isActive()).count();
            if (active != (tab == 0 ? 39 : 36)) throw new AssertionError("Wrong active slots: " + active);
            if (tab == 0 && screen.getMenu().getSlot(0).getItem().getCount() != 3)
                throw new AssertionError("Machine input did not synchronize");
        }
        if (age == 90 || age == 120 || age == 150 || age == 180) {
            int tab = (age - 90) / 30;
            Screenshot.grab(client.gameDirectory, "remastered-" + tab + ".png", client.getMainRenderTarget(), ignored -> {});
        }
        if (age == 115) {
            var panel = recipePanel(client);
            double x = (screen(client).width - 256) / 2.0 + 30;
            double y = (screen(client).height - 240) / 2.0 + 75;
            if (!screen(client).mouseScrolled(x, y, 0, -1)) throw new AssertionError("Recipe wheel ignored");
            if (!panel.mouseClicked(x, y + 56, 0)) throw new AssertionError("Scrolled recipe row not selectable");
        }
        if (age == 127) {
            var search = screen(client).children().stream().filter(net.minecraft.client.gui.components.EditBox.class::isInstance)
                .map(net.minecraft.client.gui.components.EditBox.class::cast).findFirst().orElseThrow();
            // Russian language is selected by the test launcher: live partial-name matching.
            search.setValue("");
            screen(client).setFocused(search);
            if (!screen(client).charTyped('б', 0)) throw new AssertionError("Search did not accept typed letter");
            selectBronzeRow(client);
            if (!craftButton(client).active) throw new AssertionError("Available bronze recipe not craftable after filtering");
            search.setValue("nothing_matches_this");
            if (craftButton(client).active) throw new AssertionError("Empty search result still craftable");
            search.setValue("БРОНЗ");
            selectBronzeRow(client);
            if (!craftButton(client).active) throw new AssertionError("Case-insensitive search lost bronze");
        }
        if (age == 129) Screenshot.grab(client.gameDirectory, "remastered-search.png", client.getMainRenderTarget(), ignored -> {});
        if (age == 131) craftButton(client).onPress();
        if (age == 132 && screen(client).getMenu().slots.stream().filter(slot -> slot.isActive()).count() != 39)
            throw new AssertionError("Create did not switch to Work");
        if (age == 133) {
            var search = screen(client).children().stream().filter(net.minecraft.client.gui.components.EditBox.class::isInstance)
                .map(net.minecraft.client.gui.components.EditBox.class::cast).findFirst().orElseThrow();
            search.setValue("");
        }
        if (age == 125) {
            for (int i = 0; i < 6; i++) screen(client).children().stream().filter(Button.class::isInstance).map(Button.class::cast)
                .filter(button -> button.getMessage().getString().equals(">")).findFirst().orElseThrow().onPress();
            Screenshot.grab(client.gameDirectory, "remastered-recipes-scrolled.png", client.getMainRenderTarget(), ignored -> {});
        }
        if (age == 190) {
            tab(client, 0).onPress();
            client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, 0, 0,
                ClickType.QUICK_MOVE, client.player);
        }
        if (age == 200) {
            if (screen(client).getMenu().getSlot(0).hasItem()) throw new AssertionError("Shift-click did not extract input");
            for (int i = 3; i < 39; i++) {
                if (screen(client).getMenu().getSlot(i).getItem().is(Items.COPPER_INGOT)) {
                    client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, i, 0,
                        ClickType.QUICK_MOVE, client.player);
                    return;
                }
            }
            throw new AssertionError("Extracted input missing from player inventory");
        }
        if (age == 210 && !screen(client).getMenu().getSlot(0).getItem().is(Items.COPPER_INGOT))
            throw new AssertionError("Shift-click did not return input");
        if (age == 220) {
            finished = true;
            com.mojang.logging.LogUtils.getLogger().info("IC2 Remastered checks passed: four tabs, recipe paging, synchronized slots, shift-click");
            if (Boolean.getBoolean("ic2.remastered.keepOpen")) {
                server.execute(() -> {
                    var player = server.getPlayerList().getPlayer(client.player.getUUID());
                    player.setGameMode(net.minecraft.world.level.GameType.CREATIVE);
                    var inventory = player.getInventory();
                    inventory.setItem(0, new ItemStack(Items.COPPER_INGOT, 64));
                    inventory.setItem(1, new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:ingot_tin")).orElseThrow(), 64));
                    var entity = server.overworld().getBlockEntity(POS);
                    var tag = entity.saveWithFullMetadata(server.overworld().registryAccess());
                    tag.putInt("energy", 4000);
                    entity.loadWithComponents(tag, server.overworld().registryAccess());
                    player.containerMenu.broadcastChanges();
                });
            } else client.stop();
        }
    }

    private static AbstractContainerScreen<?> screen(Minecraft client) {
        if (!(client.screen instanceof AbstractContainerScreen<?> screen)
                || !screen.getClass().getSimpleName().equals("AlloySmelterScreen"))
            throw new AssertionError("Expected Remastered alloy screen, got " + client.screen);
        return screen;
    }

    private static net.neoforged.neoforge.client.gui.widget.ScrollPanel recipePanel(Minecraft client) {
        return screen(client).children().stream()
            .filter(net.neoforged.neoforge.client.gui.widget.ScrollPanel.class::isInstance)
            .map(net.neoforged.neoforge.client.gui.widget.ScrollPanel.class::cast).findFirst()
            .orElseThrow(() -> new AssertionError("Recipes need a ready-made NeoForge ScrollPanel"));
    }

    private static Button craftButton(Minecraft client) {
        return screen(client).children().stream().filter(Button.class::isInstance).map(Button.class::cast)
            .filter(button -> button.getMessage().getString().equals("Создать")).findFirst().orElseThrow();
    }

    private static void selectBronzeRow(Minecraft client) {
        var screen = screen(client);
        if (!recipePanel(client).mouseClicked((screen.width - 256) / 2.0 + 30,
                (screen.height - 240) / 2.0 + 91, 0))
            throw new AssertionError("Filtered bronze row missing");
    }

    private static Button tab(Minecraft client, int index) {
        var screen = screen(client);
        return screen.children().stream().filter(Button.class::isInstance).map(Button.class::cast)
            .filter(button -> button.getY() == (screen.height - 240) / 2 + 24)
            .sorted(java.util.Comparator.comparingInt(Button::getX)).toList().get(index);
    }

    private static void prepare(Minecraft client) {
        var server = client.getSingleplayerServer();
        var level = server.overworld();
        var player = server.getPlayerList().getPlayer(client.player.getUUID());
        player.connection.teleport(100, 80, 98, 0, 0);
        player.getInventory().clearContent();
        level.getChunkAt(POS);
        level.setBlockAndUpdate(POS, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        level.setBlockAndUpdate(POS, Ic2Api.block(ResourceLocation.parse("ic2:alloy_smelter")).orElseThrow().defaultBlockState());
        var entity = level.getBlockEntity(POS);
        var tag = entity.saveWithFullMetadata(level.registryAccess());
        tag.putInt("energy", 4000);
        entity.loadWithComponents(tag, level.registryAccess());
        var menu = ((MenuProvider) entity).createMenu(1, player.getInventory(), player);
        verifyTransfers(menu);
        // Button IDs refer to the unfiltered server recipe order (bronze ingots = 3).
        menu.getSlot(3).set(new ItemStack(Items.COPPER_INGOT, 3));
        menu.getSlot(4).set(new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:ingot_tin")).orElseThrow()));
        if (!menu.clickMenuButton(player, 3)) throw new AssertionError("Craft button did not load available recipe");
        if (menu.getSlot(0).getItem().getCount() != 3 || menu.getSlot(2).getItem().getCount() != 1)
            throw new AssertionError("Craft button loaded incorrect quantities");
        if (menu.getSlot(3).hasItem() || menu.getSlot(4).hasItem()) throw new AssertionError("Craft button duplicated ingredients");
        if (menu.getSlot(1).hasItem()) throw new AssertionError("Craft button bypassed processing");
        if (!menu.clickMenuButton(player, 3) || menu.getSlot(0).getItem().getCount() != 3 || menu.getSlot(3).hasItem())
            throw new AssertionError("Repeated craft request duplicated ingredients");
        if (menu.clickMenuButton(player, -1) || menu.clickMenuButton(player, Integer.MAX_VALUE))
            throw new AssertionError("Invalid recipe IDs accepted");
        player.getInventory().clearContent();
        menu.getSlot(0).set(new ItemStack(Items.COPPER_INGOT, 3));
        menu.getSlot(2).set(new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:ingot_tin")).orElseThrow()));
    }

    private static void verifyTransfers(net.minecraft.world.inventory.AbstractContainerMenu menu) {
        var player = Minecraft.getInstance().getSingleplayerServer().getPlayerList()
            .getPlayer(Minecraft.getInstance().player.getUUID());
        var tin = Ic2Api.item(ResourceLocation.parse("ic2:ingot_tin")).orElseThrow();
        menu.getSlot(0).set(ItemStack.EMPTY);
        menu.getSlot(2).set(ItemStack.EMPTY);
        menu.getSlot(3).set(new ItemStack(Items.COPPER_INGOT, 2));
        menu.getSlot(4).set(new ItemStack(tin));
        if (menu.clickMenuButton(player, 3) || menu.getSlot(3).getItem().getCount() != 2)
            throw new AssertionError("Insufficient transfer accepted or consumed source");
        menu.getSlot(5).set(new ItemStack(Items.COPPER_INGOT));
        if (!menu.clickMenuButton(player, 3) || menu.getSlot(0).getItem().getCount() != 3)
            throw new AssertionError("Split-stack transfer failed");
        for (int i = 3; i < 39; i++) menu.getSlot(i).set(new ItemStack(Items.STONE, 64));
        menu.getSlot(3).set(new ItemStack(Items.COPPER_INGOT, 64));
        menu.getSlot(4).set(new ItemStack(tin, 64));
        menu.getSlot(0).set(new ItemStack(Items.REDSTONE, 2));
        menu.getSlot(2).set(new ItemStack(Items.QUARTZ));
        if (menu.clickMenuButton(player, 3) || menu.getSlot(3).getItem().getCount() != 64
                || !menu.getSlot(0).getItem().is(Items.REDSTONE))
            throw new AssertionError("Full-inventory transfer was not atomic");
        menu.getSlot(5).set(ItemStack.EMPTY);
        menu.getSlot(6).set(ItemStack.EMPTY);
        if (!menu.clickMenuButton(player, 3) || menu.getSlot(3).getItem().getCount() != 61
                || menu.getSlot(4).getItem().getCount() != 63)
            throw new AssertionError("Replacement transfer consumed wrong quantities");
        int returned = menu.slots.subList(3, 39).stream().map(slot -> slot.getItem())
            .filter(stack -> stack.is(Items.REDSTONE) || stack.is(Items.QUARTZ)).mapToInt(ItemStack::getCount).sum();
        if (returned != 3) throw new AssertionError("Old inputs lost during replacement");
        player.getInventory().clearContent();
        menu.getSlot(0).set(new ItemStack(tin));
        menu.getSlot(2).set(new ItemStack(Items.COPPER_INGOT, 3));
        if (!menu.clickMenuButton(player, 3) || !menu.getSlot(0).getItem().is(tin))
            throw new AssertionError("Already loaded reversed recipe changed");
        menu.getSlot(0).set(ItemStack.EMPTY);
        menu.getSlot(2).set(ItemStack.EMPTY);
        menu.getSlot(3).set(new ItemStack(Items.COPPER_INGOT, 64));
        menu.getSlot(4).set(new ItemStack(tin, 64));
        // Six recipes: max-mode ID is the base recipe index plus six.
        if (!menu.clickMenuButton(player, 9) || menu.getSlot(0).getItem().getCount() != 63
                || menu.getSlot(2).getItem().getCount() != 21
                || menu.getSlot(3).getItem().getCount() != 1 || menu.getSlot(4).getItem().getCount() != 43)
            throw new AssertionError("Shift create did not load the maximum fitting batches");
        if (!menu.clickMenuButton(player, 9) || menu.getSlot(0).getItem().getCount() != 63
                || menu.getSlot(3).getItem().getCount() != 1)
            throw new AssertionError("Repeated max request duplicated ingredients");
        player.getInventory().clearContent();
        menu.getSlot(0).set(ItemStack.EMPTY);
        menu.getSlot(2).set(ItemStack.EMPTY);
        menu.getSlot(3).set(new ItemStack(Items.COPPER_INGOT, 10));
        menu.getSlot(4).set(new ItemStack(tin, 2));
        if (!menu.clickMenuButton(player, 9) || menu.getSlot(0).getItem().getCount() != 6
                || menu.getSlot(2).getItem().getCount() != 2 || menu.getSlot(3).getItem().getCount() != 4)
            throw new AssertionError("Max request ignored the limiting ingredient");
        player.getInventory().clearContent();
        menu.getSlot(0).set(ItemStack.EMPTY);
        menu.getSlot(2).set(ItemStack.EMPTY);
    }
}
