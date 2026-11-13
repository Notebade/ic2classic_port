package ic2.core.machine;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Client tickers own looping sounds; unload, inactivity and world changes stop them.
 */
/** Client-owned sound loops release block entities and stop on inactivity or world unload. */
final class MachineSoundLoop extends AbstractTickableSoundInstance {
    private static final WeakHashMap<BlockEntity, MachineSoundLoop> PLAYING = new WeakHashMap<>();
    private final WeakReference<BlockEntity> entity;

    private MachineSoundLoop(BlockEntity entity, String sound) {
        super(NuclearInfrastructure.SOUNDS.get(sound).get(), SoundSource.BLOCKS, RandomSource.create());
        this.entity = new WeakReference<>(entity);
        looping = true;
        delay = 0;
        volume = 0.5F;
        x = entity.getBlockPos().getX() + 0.5;
        y = entity.getBlockPos().getY() + 0.5;
        z = entity.getBlockPos().getZ() + 0.5;
    }

    private static boolean active(BlockEntity entity) {
        return !entity.isRemoved() && entity.getLevel() == Minecraft.getInstance().level && entity.getBlockState().hasProperty(BlockStateProperties.LIT) && entity.getBlockState().getValue(BlockStateProperties.LIT);
    }

    static void clientTick(BlockEntity entity, String sound) {
        if (!active(entity)) return;
        var old = PLAYING.get(entity);
        var manager = Minecraft.getInstance().getSoundManager();
        if (old == null || old.isStopped() || entity.getLevel().getGameTime() % 20 == 0 && !manager.isActive(old)) {
            if (old != null) old.stop();
            var loop = new MachineSoundLoop(entity, sound);
            PLAYING.put(entity, loop);
            manager.play(loop);
        }
    }

    @Override
    public void tick() {
        var current = entity.get();
        if (current == null || !active(current)) stop();
    }
}
