package example;

import ic2.api.Ic2Api;
import ic2.api.item.ArmorModule;
import ic2.api.item.ModularArmor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

@net.neoforged.fml.common.EventBusSubscriber(modid = ExampleAddon.MOD_ID)
public final class ShieldModuleChecks {
    @net.neoforged.bus.api.SubscribeEvent public static void started(net.neoforged.neoforge.event.server.ServerStartedEvent event) {
        if (!Boolean.getBoolean("ic2.shield.verify")) return;
        try { run(event.getServer().overworld()); com.mojang.logging.LogUtils.getLogger().info("IC2 focused shield module checks passed"); }
        finally { event.getServer().halt(false); }
    }
    static void run(net.minecraft.server.level.ServerLevel level) {
        var player = net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var saved = player.getMainHandItem(); boolean oldSneak = player.isShiftKeyDown();
        var helmet = stack("nano_suit_helmet"); var armor = (ModularArmor) helmet.getItem();
        try {
            require(armor.install(helmet, ArmorModule.HUD, stack("energy_shield_overlay_module")), "shield installs in helmet HUD slot");
            var always = ArmorModule.id("energy_shield_always");
            require(!armor.features(helmet).contains(always), "shield defaults to hurt-only visibility");
            player.setItemInHand(InteractionHand.MAIN_HAND, helmet); player.setShiftKeyDown(true);
            helmet.use(level, player, InteractionHand.MAIN_HAND);
            require(armor.features(helmet).contains(always), "Shift+right click on held helmet enables installed shield permanently");
            helmet.use(level, player, InteractionHand.MAIN_HAND);
            require(!armor.features(helmet).contains(always), "second Shift+right click restores hurt-only visibility");
            helmet.use(level, player, InteractionHand.MAIN_HAND);
            var removed = armor.removeModule(helmet, 0, false);
            require(!removed.isEmpty() && armor.features(helmet).isEmpty(), "uninstall removes shield features");
            require(armor.install(helmet, ArmorModule.HUD, removed) && armor.features(helmet).contains(always), "shield setting survives removal and reinstallation");
        } finally { player.setItemInHand(InteractionHand.MAIN_HAND, saved); player.setShiftKeyDown(oldSneak); }
    }
    private static ItemStack stack(String id) { return new ItemStack(Ic2Api.item(ResourceLocation.parse("ic2:" + id)).orElseThrow()); }
    private static void require(boolean condition, String message) { if (!condition) throw new AssertionError(message); }
}
