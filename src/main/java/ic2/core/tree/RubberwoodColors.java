package ic2.core.tree;

import ic2.core.IC2;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;

@EventBusSubscriber(modid = IC2.MOD_ID, value = Dist.CLIENT)
final class RubberwoodColors {
    private static final int LEAVES_COLOR = 0x80A755;

    @SubscribeEvent
    static void blocks(RegisterColorHandlersEvent.Block event) {
        event.register((state, level, pos, tint) -> LEAVES_COLOR, Rubberwood.LEAVES.get());
    }

    @SubscribeEvent
    static void items(RegisterColorHandlersEvent.Item event) {
        event.register((stack, tint) -> LEAVES_COLOR, Rubberwood.LEAVES.get());
    }
}
