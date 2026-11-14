package ic2.core.equipment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** One moving loop per player, shared by both hands, stopped on unequip or world change. */
@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class NanoSaberAudio {
    private static final Map<UUID, Hum> LOOPS = new HashMap<>();
    private static int miningTicks;

    static void miningSound() {
        var client = Minecraft.getInstance();
        if (!client.isSameThread() || client.player == null || ++miningTicks % 4 != 0) return;
        client.player.playSound(NanoSaber.soundEvent(1 + client.player.getRandom().nextInt(3)), 1, 1);
    }

    private static boolean holding(Player player) {
        for (var stack : player.getHandSlots()) if (stack.getItem() instanceof NanoSaber && NanoSaber.active(stack)) return true;
        return false;
    }

    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        LOOPS.values().removeIf(sound -> {
            if (sound.valid()) return false;
            sound.finish(); client.getSoundManager().stop(sound); return true;
        });
        if (client.level == null) return;
        for (var player : client.level.players()) {
            if (!player.isAlive() || !holding(player) || LOOPS.containsKey(player.getUUID())) continue;
            var sound = new Hum(player);
            LOOPS.put(player.getUUID(), sound);
            client.getSoundManager().play(sound);
        }
    }

    private static final class Hum extends AbstractTickableSoundInstance {
        private final Player owner;
        Hum(Player owner) {
            super(NanoSaber.soundEvent(4), SoundSource.PLAYERS, owner.getRandom());
            this.owner = owner; looping = true; delay = 0; volume = 1;
            tick();
        }
        boolean valid() {
            return !isStopped() && owner.isAlive() && !owner.isRemoved()
                && owner.level() == Minecraft.getInstance().level && holding(owner);
        }
        void finish() { stop(); }
        @Override public void tick() {
            if (!valid()) { stop(); return; }
            x = owner.getX(); y = owner.getY(); z = owner.getZ();
        }
    }
}
