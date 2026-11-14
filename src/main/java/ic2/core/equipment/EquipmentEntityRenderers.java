package ic2.core.equipment;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Minimal native renderer binding; graphics stage owns the replacement appearance. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class EquipmentEntityRenderers {
    @SubscribeEvent public static void setup(net.neoforged.fml.event.lifecycle.FMLClientSetupEvent event) {
        var id = net.minecraft.resources.ResourceLocation.parse("ic2:nano_saber");
        event.enqueueWork(() -> net.minecraft.client.renderer.item.ItemProperties.register(
            net.minecraft.core.registries.BuiltInRegistries.ITEM.get(id), net.minecraft.resources.ResourceLocation.parse("ic2:active"),
            (stack, level, entity, seed) -> NanoSaber.active(stack) ? 1 + java.util.concurrent.ThreadLocalRandom.current().nextInt(2) : 0));
        NanoSaber.miningSound = NanoSaberAudio::miningSound;
        NanoSaber.clientAlt = EquipmentKeys::altDown;
        ModularSuitItem.clientSideInventory = EquipmentKeys::sideInventoryDown;
        event.enqueueWork(() -> net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(EquipmentEntityRenderers::drawEnergy));
    }
    private static void drawEnergy(net.neoforged.neoforge.client.event.RenderGuiEvent.Post event) {
        var client = net.minecraft.client.Minecraft.getInstance();
        if (client.player == null || client.options.hideGui) return;
        var helmet = client.player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
        if (!(helmet.getItem() instanceof ic2.api.item.ModularArmor armor)) return;
        var features = armor.features(helmet);
        if (!features.contains(ic2.api.item.ArmorModule.id("energy_hud"))) return;
        int y = 6;
        for (var stack : client.player.getArmorSlots()) {
            if (!(stack.getItem() instanceof ic2.api.item.ElectricItem item) || item.capacity(stack) <= 0) continue;
            event.getGuiGraphics().drawString(client.font, net.minecraft.network.chat.Component.translatable("gui.ic2.energy_hud",
                stack.getHoverName(), item.charge(stack), item.capacity(stack)), 6, y, 0xFFFFFF);
            y += 10;
        }
    }
    @SubscribeEvent public static void layers(EntityRenderersEvent.AddLayers event) {
        for (var skin : event.getSkins()) {
            var renderer = event.<net.minecraft.client.renderer.entity.player.PlayerRenderer>getSkin(skin);
            if (renderer != null) renderer.addLayer(new EnergyShieldLayer(renderer));
        }
    }
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(LaserShot.TYPE.get(), LaserShotRenderer::new);
        event.registerEntityRenderer(CarbonBoats.TYPE.get(), context -> new net.minecraft.client.renderer.entity.BoatRenderer(context, false));
    }
}
