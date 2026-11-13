package ic2.core.equipment;

import ic2.api.item.EquipmentInput;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** Gameplay input transport only; no screens or HUD rendering. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public final class EquipmentKeys {
    private static final KeyMapping MODE = new KeyMapping("key.ic2.mode", GLFW.GLFW_KEY_M, "key.categories.ic2");
    private static final KeyMapping HUD = new KeyMapping("key.ic2.hud", GLFW.GLFW_KEY_H, "key.categories.ic2");
    private static final KeyMapping BOOST = new KeyMapping("key.ic2.boost", GLFW.GLFW_KEY_LEFT_CONTROL, "key.categories.ic2");
    private static final KeyMapping ALT = new KeyMapping("key.ic2.alt", GLFW.GLFW_KEY_LEFT_ALT, "key.categories.ic2");
    private static EquipmentInput last = EquipmentInput.NONE;
    private static int ticks;
    @SubscribeEvent public static void register(RegisterKeyMappingsEvent event) {
        event.register(MODE); event.register(HUD); event.register(BOOST); event.register(ALT);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(EquipmentKeys::tick);
    }
    private static void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        if (client.player == null || client.getConnection() == null) { last = EquipmentInput.NONE; ticks = 0; return; }
        var input = client.screen != null ? EquipmentInput.NONE : new EquipmentInput(client.options.keyJump.isDown(), BOOST.isDown(), client.options.keyUp.isDown(), HUD.isDown(), MODE.isDown(), ALT.isDown());
        if (!input.equals(last) || ++ticks >= 10) { EquipmentInputPayload.send(input); last = input; ticks = 0; }
    }
}
