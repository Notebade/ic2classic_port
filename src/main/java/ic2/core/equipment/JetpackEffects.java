package ic2.core.equipment;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

@EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
public final class JetpackEffects {
    private static final Map<UUID, EngineSound> ENGINES = new HashMap<>();
    private record State(Item item, int mode) {}
    private static State state(Player player) {
        var stack = player.getItemBySlot(EquipmentSlot.CHEST);
        if (stack.getItem() instanceof ic2.api.item.ModularArmor armor) {
            stack = armor.modules(stack).stream().map(ic2.api.item.InstalledArmorModule::stack)
                .filter(module -> module.getItem() instanceof JetpackFlight.Pack).findFirst().orElse(ItemStack.EMPTY);
        }
        if (!(stack.getItem() instanceof JetpackFlight.Pack)) return new State(stack.getItem(), 2);
        var data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int mode = data.contains("LastUseMode") && !data.getBoolean("disabled") ? data.getInt("LastUseMode") : 2;
        return new State(stack.getItem(), mode >= 0 && mode < 2 ? mode : 2);
    }
    @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
        var client = Minecraft.getInstance();
        ENGINES.values().removeIf(sound -> {
            if (sound.valid()) return false;
            sound.finish(); client.getSoundManager().stop(sound); return true;
        });
        if (client.level == null || client.isPaused()) return;
        for (var player : client.level.players()) {
            var state = state(player);
            if (!player.isAlive() || state.mode == 2) continue;
            if (!ENGINES.containsKey(player.getUUID())) {
                var sound = new EngineSound(player, state); ENGINES.put(player.getUUID(), sound); client.getSoundManager().play(sound);
            }
            boolean elytra = player.isFallFlying();
            for (double offset : new double[]{-.65, .65}) {
                double yaw = Math.toRadians(player.yBodyRot) - offset;
                double scale = elytra ? .1 : .25;
                client.level.addParticle(ParticleTypes.SMALL_FLAME, player.getX() + Math.sin(-yaw - Math.PI) * scale,
                    player.getY() + (elytra ? .2 : .75), player.getZ() + Math.cos(-yaw - Math.PI) * scale, 0, 0, 0);
            }
        }
    }
    private static final class EngineSound extends AbstractTickableSoundInstance {
        private final Player owner;
        private final State playing;
        EngineSound(Player owner, State playing) {
            super(JetpackFlight.sound(playing.mode), SoundSource.PLAYERS, owner.getRandom());
            this.owner = owner; this.playing = playing; looping = true; delay = 0; volume = 1; tick();
        }
        boolean valid() {
            return !isStopped() && owner.isAlive() && !owner.isRemoved() && owner.level() == Minecraft.getInstance().level && playing.equals(state(owner));
        }
        void finish() { stop(); }
        @Override public void tick() {
            if (!valid()) { stop(); return; }
            x = owner.getX(); y = owner.getY(); z = owner.getZ();
        }
    }
}
