package ic2.core.machine;

import ic2.core.IC2;
import ic2.core.item.PortableBatteryItem;
import java.lang.ref.WeakReference;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = IC2.MOD_ID, value = Dist.CLIENT)
public final class BatterySoundLoop extends AbstractTickableSoundInstance {
    private static BatterySoundLoop playing;
    private final WeakReference<LocalPlayer> player;
    private BatterySoundLoop(LocalPlayer player) {
        super(NuclearInfrastructure.SOUNDS.get("battery_charging").get(), SoundSource.PLAYERS, RandomSource.create());
        this.player = new WeakReference<>(player); looping = true; delay = 0; volume = .5F;
        x = player.getX(); y = player.getY(); z = player.getZ();
    }
    private static boolean active(LocalPlayer player) {
        return player != null && player.isAlive() && player.isUsingItem()
            && player.getUseItem().getItem() instanceof PortableBatteryItem battery && battery.charge(player.getUseItem()) > 0;
    }
    @SubscribeEvent public static void clientTick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        if (playing != null && (playing.isStopped() || !active(client.player) || playing.player.get() != client.player)) { playing.stop(); playing = null; }
        if (active(client.player) && playing == null) {
            playing = new BatterySoundLoop(client.player); client.getSoundManager().play(playing);
        }
    }
    @Override public void tick() {
        var current = player.get();
        if (current != Minecraft.getInstance().player || !active(current)) { stop(); return; }
        x = current.getX(); y = current.getY(); z = current.getZ();
    }
}
