package ic2.core.machine;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/** Client-owned sound loops release block entities and stop on inactivity or world unload. */
final class MachineSoundLoop extends AbstractTickableSoundInstance {
    private static final WeakHashMap<BlockEntity, java.util.Map<String, MachineSoundLoop>> PLAYING = new WeakHashMap<>();
    private final WeakReference<BlockEntity> entity;
    private final String sound;

    private MachineSoundLoop(BlockEntity entity, String sound) {
        super(NuclearInfrastructure.SOUNDS.get(sound).get(), SoundSource.BLOCKS, RandomSource.create());
        this.entity = new WeakReference<>(entity);
        this.sound = sound;
        looping = true;
        delay = 0;
        volume = 0.5F;
        x = entity.getBlockPos().getX() + 0.5;
        y = entity.getBlockPos().getY() + 0.5;
        z = entity.getBlockPos().getZ() + 0.5;
    }

    private static boolean active(BlockEntity entity, String sound) {
        if (entity.isRemoved() || entity.getLevel() != Minecraft.getInstance().level
            || !entity.getBlockState().hasProperty(BlockStateProperties.LIT) || !entity.getBlockState().getValue(BlockStateProperties.LIT)) return false;
        if (sound.equals("reactor_operating")) return entity instanceof MachineEntity machine && machine.reactorSoundLevel() > 0;
        if (!sound.startsWith("geiger_")) return true;
        int tier = switch (sound) { case "geiger_low" -> 1; case "geiger_med" -> 2; default -> 3; };
        return entity instanceof MachineEntity machine && machine.reactorSoundLevel() == tier;
    }

    static void clientTick(BlockEntity entity, String sound) {
        if (sound.equals("reactor_operating")) {
            for (var geiger : new String[]{"geiger_low", "geiger_med", "geiger_high"}) clientTick(entity, geiger);
        }
        if (!active(entity, sound)) return;
        var sounds = PLAYING.computeIfAbsent(entity, key -> new java.util.HashMap<>());
        var old = sounds.get(sound);
        var manager = Minecraft.getInstance().getSoundManager();
        if (old == null || old.isStopped() || entity.getLevel().getGameTime() % 20 == 0 && !manager.isActive(old)) {
            if (old != null) old.stop();
            var loop = new MachineSoundLoop(entity, sound);
            sounds.put(sound, loop);
            manager.play(loop);
        }
    }

    @Override
    public void tick() {
        var current = entity.get();
        if (current == null || !active(current, sound)) stop();
    }
}
