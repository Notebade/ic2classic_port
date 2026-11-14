package example;

import ic2.api.Ic2Api;
import ic2.api.item.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = ExampleAddon.MOD_ID, value = Dist.CLIENT)
public final class FoodClientCheck {
    private static int ticks;
    private static ItemStack chest;
    private static volatile Throwable failure;
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("ic2.visual.verify") || !System.getProperty("ic2.equipment.case", "").equals("food")) return;
        var client = Minecraft.getInstance(); var server = client.getSingleplayerServer();
        if (client.player == null || server == null || client.isPaused()) return;
        if (failure != null) throw new AssertionError("Food client fixture failed", failure);
        int age = ++ticks; var id = client.player.getUUID();
        if (age == 20) server.execute(() -> {
            try {
                FoodStorageChecks.run(server.overworld());
                var player = server.getPlayerList().getPlayer(id);
                player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL); player.setInvulnerable(true);
                player.getInventory().clearContent(); player.getFoodData().setFoodLevel(20);
                var helmetArmor = (ModularArmor) stack("nano_suit_helmet").getItem();
                var helmet = helmetArmor.defaultArmor();
                require(helmetArmor.install(helmet, ArmorModule.GENERIC, stack("auto_feeder_module")), "feeder installs in Nano helmet");
                player.setItemSlot(EquipmentSlot.HEAD, helmet);
                var armor = (ModularArmor) stack("nano_suit_chestplate").getItem(); chest = armor.defaultArmor();
                require(armor.install(chest, ArmorModule.STORAGE, stack("food_storage_module")), "food module installs");
                player.setItemInHand(InteractionHand.MAIN_HAND, chest);
                player.getInventory().setItem(1, stack("filled_tin_can").copyWithCount(2));
                player.setShiftKeyDown(true); chest.use(player.level(), player, InteractionHand.MAIN_HAND); player.setShiftKeyDown(false);
                require(player.containerMenu.getSlot(0).getItem().isEmpty(), "food menu starts empty without exposing armor modules");
                require(!player.containerMenu.getSlot(0).mayPlace(new ItemStack(net.minecraft.world.item.Items.STONE))
                    && !player.containerMenu.getSlot(2).mayPlace(stack("filled_tin_can")), "food menu rejects invalid items and filled cans in output slots");
                com.mojang.logging.LogUtils.getLogger().info("IC2 food client menu opened with intact armor modules");
            } catch (Throwable error) { failure = error; }
        });
        if (age == 60) {
            require(client.player.containerMenu.slots.size() == 40, "food menu synced to client");
            client.gameMode.handleInventoryMouseClick(client.player.containerMenu.containerId, 32, 0, ClickType.QUICK_MOVE, client.player);
        }
        if (age == 90) {
            require(client.player.containerMenu.getSlot(0).getItem().getCount() == 2, "client shift-click stores both cans");
            capture(client, "food-storage-filled.png");
        }
        if (age == 110) {
            client.player.closeContainer();
            server.execute(() -> {
                try {
                    var player = server.getPlayerList().getPlayer(id);
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY); player.setItemSlot(EquipmentSlot.CHEST, chest);
                    ((ModularArmor) player.getItemBySlot(EquipmentSlot.HEAD).getItem()).setCharge(player.getItemBySlot(EquipmentSlot.HEAD), 2000);
                    player.getFoodData().setFoodLevel(16); player.setHealth(16);
                } catch (Throwable error) { failure = error; }
            });
        }
        if (age == 135) server.execute(() -> {
            try {
                var player = server.getPlayerList().getPlayer(id); checkFed(player, "armor storage");
                require(chest.get(FoodStorageChecks.foodComponent()).getStackInSlot(2).getCount() == 2, "empty cans retained in armor output slots");
                require(((ModularArmor) chest.getItem()).modules(chest).size() == 2, "battery and storage module survive feeding");
                player.setItemSlot(EquipmentSlot.CHEST, ItemStack.EMPTY); player.setItemInHand(InteractionHand.MAIN_HAND, chest);
                player.setShiftKeyDown(true); chest.use(player.level(), player, InteractionHand.MAIN_HAND); player.setShiftKeyDown(false);
                com.mojang.logging.LogUtils.getLogger().info("IC2 food client armor storage feeding passed");
            } catch (Throwable error) { failure = error; }
        });
        if (age == 165) capture(client, "food-storage-empty-cans.png");
        if (age == 185) {
            client.player.closeContainer();
            server.execute(() -> {
                try {
                    var player = server.getPlayerList().getPlayer(id);
                    var box = stack("carbon_box"); require(((Toolbox) box.getItem()).store(box, 0, stack("filled_tin_can").copyWithCount(2)), "put cans in toolbox");
                    player.setItemInHand(InteractionHand.MAIN_HAND, box);
                    ((ModularArmor) player.getItemBySlot(EquipmentSlot.HEAD).getItem()).setCharge(player.getItemBySlot(EquipmentSlot.HEAD), 2000);
                    player.getFoodData().setFoodLevel(16); player.setHealth(16);
                } catch (Throwable error) { failure = error; }
            });
        }
        if (age == 210) server.execute(() -> {
            try {
                var player = server.getPlayerList().getPlayer(id); checkFed(player, "toolbox");
                var box = player.getMainHandItem();
                require(((Toolbox) box.getItem()).stored(box, 0).isEmpty(), "toolbox food consumed");
                require(player.getInventory().items.stream().filter(item -> item.is(stack("tin_can").getItem())).mapToInt(ItemStack::getCount).sum() == 2,
                    "toolbox empty cans returned to inventory");
                com.mojang.logging.LogUtils.getLogger().info("IC2 food client toolbox feeding passed");
            } catch (Throwable error) { failure = error; }
        });
        if (age == 235) {
            client.setScreen(new net.minecraft.client.gui.screens.inventory.InventoryScreen(client.player));
            capture(client, "food-toolbox-result.png");
            com.mojang.logging.LogUtils.getLogger().info("IC2 equipment visual fixture ready: food storage and toolbox feeding passed");
        }
    }
    private static void checkFed(net.minecraft.server.level.ServerPlayer player, String source) {
        var helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        require(player.getFoodData().getFoodLevel() == 20 && player.getHealth() == 20 && ((ModularArmor) helmet.getItem()).charge(helmet) == 0,
            source + " restores hunger and health for exactly 2000 EU");
    }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow()); }
    private static void capture(Minecraft client, String file) { net.minecraft.client.Screenshot.grab(client.gameDirectory, file, client.getMainRenderTarget(), message -> {}); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
