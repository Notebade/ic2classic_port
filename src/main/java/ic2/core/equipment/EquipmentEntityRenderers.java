package ic2.core.equipment;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Minimal native renderer binding; graphics stage owns the replacement appearance. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class EquipmentEntityRenderers {
    @SubscribeEvent public static void register(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(LaserShot.TYPE.get(), LaserShotRenderer::new);
        event.registerEntityRenderer(CarbonBoats.TYPE.get(), context -> new net.minecraft.client.renderer.entity.BoatRenderer(context, false));
    }
}
