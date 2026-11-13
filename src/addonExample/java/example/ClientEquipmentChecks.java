package example;

import ic2.api.Ic2Api;
import ic2.api.item.*;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;

/** Real network/client checks run alongside the isolated audio fixture, without opening equipment screens. */
final class ClientEquipmentChecks {
    static void tick(Minecraft client, int age) {
        var server = client.getSingleplayerServer();
        if (age == 1) server.execute(() -> {
            var player = server.getPlayerList().getPlayer(client.player.getUUID()); var goggles = stack("nightvision_goggles");
            ((ElectricItem) goggles.getItem()).setCharge(goggles, 1000); player.setItemSlot(EquipmentSlot.HEAD, goggles);
            var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse("ic2:carbon_boat"));
            var boat = type.create(server.overworld()); boat.setPos(100.5, 80, 99.5); server.overworld().addFreshEntity(boat);
        });
        if (age == 50) {
            boolean found = false;
            for (var entity : client.level.entitiesForRendering()) {
                if (!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).equals(ResourceLocation.parse("ic2:carbon_boat"))) continue;
                net.minecraft.client.renderer.entity.EntityRenderer<?> renderer = client.getEntityRenderDispatcher().getRenderer(entity);
                require(renderer instanceof net.minecraft.client.renderer.entity.BoatRenderer, "native carbon boat spawn packet resolves a valid client renderer"); found = true;
            }
            require(found, "server carbon boat reaches the real client");
        }
        if (age == 60) { key(client, "key.ic2.hud", true); key(client, "key.ic2.mode", true); }
        if (age == 64) { key(client, "key.ic2.hud", false); key(client, "key.ic2.mode", false); }
        if (age == 110) {
            var goggles = client.player.getItemBySlot(EquipmentSlot.HEAD);
            require(goggles.getItem() instanceof ConfigurableTool mode && mode.mode(goggles).equals("on") && client.player.hasEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION), "real equipment key input reaches server and syncs enabled goggles/night vision back to client");
            require(((ElectricItem) goggles.getItem()).charge(goggles) < 1000, "real server consumes goggles EU while worn");
            server.execute(() -> {
                var player = server.getPlayerList().getPlayer(client.player.getUUID()); var shell = stack("nano_suit_helmet"); var item = (ModularArmor) shell.getItem(); var armor = item.defaultArmor();
                require(item.install(armor, ArmorModule.GENERIC, new ItemStack(ExampleAddon.ARMOR_TEST_MODULE.get())), "server installs separately compiled addon module"); player.setItemSlot(EquipmentSlot.HEAD, armor);
            });
        }
        if (age == 160) {
            var armor = client.player.getItemBySlot(EquipmentSlot.HEAD);
            require(armor.getItem() instanceof ModularArmor item && item.capacity(armor) == 101000 && item.modules(armor).size() == 2 && item.features(armor).contains(ResourceLocation.parse("exampleaddon:fixture")), "module custody, per-stack EU and addon features sync to actual client");
            com.mojang.logging.LogUtils.getLogger().info("IC2 client equipment checks passed: carbon boat spawn/renderer, real key payload, goggles EU/effect and modular armor component sync");
        }
    }
    private static void key(Minecraft client, String name, boolean pressed) {
        java.util.Arrays.stream(client.options.keyMappings).filter(key -> key.getName().equals(name)).findFirst().orElseThrow(() -> new AssertionError("equipment key registered: " + name)).setDown(pressed);
    }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow()); }
    private static void require(boolean value, String message) { if (!value) throw new AssertionError(message); }
}
